"""Exercise the real source-check entry point on disposable runtime-tree copies."""
import argparse
import contextlib
import io
import pathlib
import shutil
import sys
import tempfile
import unittest
from unittest import mock

import reval_source

ARCHIVE = None


class SourceParityMutationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # The same pinned archive and checksum gate as the production checker.
        cls.upstream_files = reval_source.upstream(ARCHIVE)

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='nightlegion-parity-')
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        shutil.copytree(reval_source.ROOT/'src/main', self.root/'src/main')
        shutil.copy2(reval_source.ROOT/'LICENSE', self.root/'LICENSE')
        shutil.copytree(reval_source.ROOT/'LICENSES', self.root/'LICENSES')

    def check(self):
        # Invoke the actual CLI implementation, not a separate comparison stub.
        # Cache only the already checksum-verified archive to avoid redownloading.
        with mock.patch.object(reval_source, 'ROOT', self.root), \
             mock.patch.object(reval_source, 'upstream', return_value=self.upstream_files), \
             contextlib.redirect_stdout(io.StringIO()):
            reval_source.main([])

    def add_unrelated_field(self, relative):
        path = self.root/relative
        source = path.read_bytes()
        before, closing = source.rsplit(b'}', 1)
        path.write_bytes(before+b'\n    private static final int UNRELATED_CHANGE = 42;\n}'+closing)

    def test_current_source_passes_the_cli(self):
        self.check()
        self.assertTrue((self.root/'build/source-parity.json').is_file())

    def test_unrelated_code_in_preserved_notifier_fails_the_cli(self):
        relative = reval_source.JAVA+'notifiers/QuestNotifier.java'
        self.add_unrelated_field(relative)
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*QuestNotifier.java'):
            self.check()

    def test_unrelated_code_in_adapted_file_fails_the_cli(self):
        relative = reval_source.JAVA+'collectionlog/CollectionLogManager.java'
        self.add_unrelated_field(relative)
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*CollectionLogManager.java'):
            self.check()

    def test_changed_approved_guard_fails_the_cli(self):
        path = self.root/(reval_source.JAVA+'collectionlog/CollectionLogManager.java')
        source = path.read_bytes()
        self.assertEqual(1, source.count(b'itemCount <= 0'))
        path.write_bytes(source.replace(b'itemCount <= 0', b'itemCount < 0'))
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*CollectionLogManager.java'):
            self.check()

    def test_changed_authentication_boundary_fails_the_cli(self):
        self.add_unrelated_field(reval_source.AUTH_PATH)
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*NightLegionAuthentication.java'):
            self.check()

    def test_removed_stale_account_guard_fails_the_cli(self):
        path = self.root/(reval_source.JAVA+'ui/ProfilePanel.java')
        source = path.read_bytes()
        guard = b'if (generation != accountRequestGeneration.get()) return;'
        self.assertGreater(source.count(guard), 0)
        path.write_bytes(source.replace(guard, b'// Removed stale account guard', 1))
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*ProfilePanel.java'):
            self.check()

    def test_removed_sync_fingerprint_guard_fails_the_cli(self):
        path = self.root/(reval_source.JAVA+'notifiers/SyncNotifier.java')
        source = path.read_bytes()
        guard = b'result != SyncResult.FAILED && matchesFingerprint(response, fingerprint)'
        self.assertEqual(1, source.count(guard))
        path.write_bytes(source.replace(guard, b'result != SyncResult.FAILED'))
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*SyncNotifier.java'):
            self.check()

    def assert_runtime_mutation_rejected(self, relative, before, after):
        path = self.root/(reval_source.JAVA+relative)
        source = path.read_bytes().replace(b'\r\n', b'\n')
        self.assertEqual(1, source.count(before))
        path.write_bytes(source.replace(before, after, 1))
        with self.assertRaisesRegex(AssertionError,
                'Unapproved upstream deviations:.*'+path.name):
            self.check()

    def test_unrelated_code_in_new_loot_adaptation_fails_the_cli(self):
        self.add_unrelated_field(reval_source.JAVA+'notifiers/LootNotifier.java')
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*LootNotifier.java'):
            self.check()

    def test_changed_loot_receipt_identity_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('notifiers/LootNotifier.java',
            b'lootData.put("eventId", UUID.randomUUID().toString());',
            b'lootData.put("eventId", "same-for-every-drop");')

    def test_removed_pending_loot_reset_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('notifiers/LootNotifier.java',
            b'\t\tpendingLoot.clear();', b'\t\t// pending loot no longer reset')

    def test_removed_diary_account_guard_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('notifiers/DiaryNotifier.java',
            b'|| client.getAccountHash() != accountHash) return true;',
            b') return true;')

    def test_removed_session_failure_release_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('session/SessionTracker.java',
            b'error -> confirmDelivered(id, null));', b'error -> {});')

    def test_removed_api_account_generation_guard_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('api/RevalApiService.java',
            b'if (generation != accountGeneration) return;',
            b'// account generation guard removed')

    def test_removed_logout_ownership_reset_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('RevalClanPlugin.java',
            b'\t\t\tcase LOGIN_SCREEN: {\n\t\t\t\tcollectionLogManager.clearObtainedItems();',
            b'\t\t\tcase LOGIN_SCREEN: {')

    def test_changed_fractional_ledger_model_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('api/account/AccountResponse.java',
            b'private Double pointsChange;', b'private Integer pointsChange;')

    def test_changed_point_precision_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('util/NumberFmt.java',
            b'new DecimalFormat("#,##0.####", symbols)',
            b'new DecimalFormat("#,##0", symbols)')

    def test_unrelated_code_in_resource_loader_fails_the_cli(self):
        self.add_unrelated_field(reval_source.JAVA+'util/UIAssetLoader.java')
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*UIAssetLoader.java'):
            self.check()

    def test_removed_resource_stream_closure_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('util/UIAssetLoader.java',
            b'\n      try (InputStream imageStream = getClass().getResourceAsStream(resourcePath)) {',
            b'\n      try {\n        InputStream imageStream = getClass().getResourceAsStream(resourcePath);')

    def test_removed_missing_resource_fallback_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('util/UIAssetLoader.java',
            b'\n        if (imageStream == null) {\n          return null;\n        }',
            b'\n        // Missing-resource fallback removed')

    def test_removed_native_acknowledgement_guard_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('notifiers/SyncNotifier.java',
            b'(response.has("accepted") && !accepted)', b'false')

    def test_removed_achievements_response_guard_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('ui/AchievementsPanel.java',
            b'response -> SwingUtilities.invokeLater(() -> {\n\t\t\t\tif (generation != requestGeneration) return;',
            b'response -> SwingUtilities.invokeLater(() -> {')

    def test_removed_diary_response_guard_fails_the_cli(self):
        self.assert_runtime_mutation_rejected('ui/DiaryPanel.java',
            b'response -> SwingUtilities.invokeLater(() -> {\n\t\t\t\tif (generation != requestGeneration) return;',
            b'response -> SwingUtilities.invokeLater(() -> {')

    def test_changed_brand_icon_fails_the_cli(self):
        path = self.root/reval_source.ICON
        path.write_bytes(path.read_bytes()+b'unapproved')
        with self.assertRaisesRegex(AssertionError, 'Unapproved upstream deviations:.*reval.png'):
            self.check()

    def test_extra_runtime_file_fails_the_cli(self):
        (self.root/(reval_source.JAVA+'Unexpected.java')).write_text('class Unexpected {}\n')
        with self.assertRaisesRegex(AssertionError, 'extra.*Unexpected.java'):
            self.check()

    def test_missing_runtime_file_fails_the_cli(self):
        (self.root/(reval_source.JAVA+'notifiers/BaseNotifier.java')).unlink()
        with self.assertRaisesRegex(AssertionError, 'missing.*BaseNotifier.java'):
            self.check()

    def test_windows_text_line_endings_pass_the_cli(self):
        for path in (self.root/'src/main').rglob('*.java'):
            path.write_bytes(path.read_bytes().replace(b'\r\n', b'\n').replace(b'\n', b'\r\n'))
        self.check()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument('--archive')
    args, remaining = parser.parse_known_args()
    ARCHIVE = args.archive
    unittest.main(argv=[sys.argv[0], *remaining])
