"""Exact ProfilePanel account-boundary corrections, applied after identity changes."""


def adapt_profile_account_boundary(source, one):
    source = one(source, 'import java.util.function.Consumer;',
                 'import java.util.function.Consumer;\nimport java.util.concurrent.atomic.AtomicLong;')
    source = one(source, '\tprivate boolean isLoading = false;',
                 '\tprivate boolean isLoading = false;\n\tprivate final AtomicLong accountRequestGeneration = new AtomicLong();')
    source = one(source, '''\t\t\t\t\tif (currentAccount != null) {
\t\t\t\t\t\tSwingUtilities.invokeLater(this::buildProfile);
\t\t\t\t\t}''', '''\t\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\t\tif (currentAccount != null) buildProfile();
\t\t\t\t\t});''')
    source = one(source, '\tpublic void onLoggedOut() {', '''\tpublic void onLoggedOut() {
\t\taccountRequestGeneration.incrementAndGet();
\t\tisLoading = false;
\t\tcurrentAccount = null;
\t\tpointsLog = null;
\t\tdisposeAlbum();''')
    for signature in ('loadAccount(long accountHash)', 'loadAccountById(int osrsAccountId)'):
        anchor = '\tpublic void '+signature+' {\n\t\tif (isLoading) return;\n\t\tisLoading = true;'
        source = one(source, anchor, anchor+'\n\t\tlong generation = accountRequestGeneration.incrementAndGet();')
    source = one(source, '''\t\tapiService.fetchAccount(accountHash,
\t\t\tresponse -> {
\t\t\t\tisLoading = false;
\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\tcurrentAccount = response.getData();
\t\t\t\t\tif (currentAccount != null) {
\t\t\t\t\t\tpointsLog = currentAccount.getPointsLog();''', '''\t\tapiService.fetchAccount(accountHash,
\t\t\tresponse -> {
\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\tif (generation != accountRequestGeneration.get()) return;
\t\t\t\t\tisLoading = false;
\t\t\t\t\tcurrentAccount = response.getData();
\t\t\t\t\tpointsLog = currentAccount != null ? currentAccount.getPointsLog() : null;
\t\t\t\t\tif (currentAccount != null) {''')
    source = one(source, '''\t\tapiService.fetchAccountById(osrsAccountId,
\t\t\tresponse -> {
\t\t\t\tisLoading = false;
\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\tcurrentAccount = response.getData();
\t\t\t\t\tif (currentAccount != null) pointsLog = currentAccount.getPointsLog();''', '''\t\tapiService.fetchAccountById(osrsAccountId,
\t\t\tresponse -> {
\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\tif (generation != accountRequestGeneration.get()) return;
\t\t\t\t\tisLoading = false;
\t\t\t\t\tcurrentAccount = response.getData();
\t\t\t\t\tpointsLog = currentAccount != null ? currentAccount.getPointsLog() : null;''')
    for message in ('Failed to fetch account data', 'Player not found'):
        source = one(source, '''\t\t\t\tisLoading = false;
\t\t\t\tSwingUtilities.invokeLater(() -> showError(error.getMessage() != null ? error.getMessage() : "'''+message+'''"));''', '''\t\t\t\tSwingUtilities.invokeLater(() -> {
\t\t\t\t\tif (generation != accountRequestGeneration.get()) return;
\t\t\t\t\tisLoading = false;
\t\t\t\t\tshowError(error.getMessage() != null ? error.getMessage() : "'''+message+'''");
\t\t\t\t});''')
    return source
