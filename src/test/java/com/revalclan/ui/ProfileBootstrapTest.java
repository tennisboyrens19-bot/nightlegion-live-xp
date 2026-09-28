package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.RevalApiService;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.points.PointsResponse;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class ProfileBootstrapTest {
	private final AtomicReference<Throwable> uiError = new AtomicReference<>();
	private Thread.UncaughtExceptionHandler previousHandler;
	@Before public void captureUiErrors() {
		previousHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> uiError.set(error));
	}
	@After public void verifyUiErrors() throws Exception {
		SwingUtilities.invokeAndWait(() -> {});
		Thread.setDefaultUncaughtExceptionHandler(previousHandler);
		if (uiError.get() != null) throw new AssertionError("Profile rendering failed", uiError.get());
	}
	@Test public void ownProfileStartsBothRequestsBeforeAccountResponse() throws Exception {
		checkLoad(ProfilePanel::loadAccount);
	}
	@Test public void otherProfileStartsBothRequestsBeforeAccountResponse() throws Exception {
		checkLoad((panel, id) -> panel.loadAccountById((int) id));
	}
	@Test public void loadedConfigurationIsReusedAcrossAccountLoads() throws Exception {
		FakeApi api = new FakeApi();
		ProfilePanel[] panel = new ProfilePanel[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new ProfilePanel();
			panel[0].init(api, null, null, null, null, null);
			panel[0].loadAccount(42);
			api.points.accept(new Gson().fromJson("{\"status\":\"success\",\"data\":{\"ranks\":[]}}", PointsResponse.class));
			api.account.accept(new AccountResponse());
		});
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> panel[0].loadAccount(43));
		assertEquals(1, api.pointsRequests);
	}
	@Test public void logoutDiscardsLateAccountResponseAndAllowsNextAccountToLoad() throws Exception {
		FakeApi api = new FakeApi();
		ProfilePanel[] panel = new ProfilePanel[1];
		AtomicReference<Consumer<AccountResponse>> oldResponse = new AtomicReference<>();
		AtomicReference<Consumer<Exception>> oldError = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new ProfilePanel();
			panel[0].init(api, null, null, null, null, null);
			panel[0].loadAccount(42);
			oldResponse.set(api.account);
			oldError.set(api.error);
			panel[0].onLoggedOut();
			panel[0].loadAccount(43);
			api.account.accept(account("Current", "New rank"));
		});
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals("New rank", panel[0].getClanRank());
		oldResponse.get().accept(account("Previous", "Old rank"));
		oldError.get().accept(new RuntimeException("Late previous-account failure"));
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals("New rank", panel[0].getClanRank());
		SwingUtilities.invokeAndWait(panel[0]::onLoggedOut);
		assertFalse(panel[0].isAccountLoaded());
		oldResponse.get().accept(account("Previous", "Old rank"));
		SwingUtilities.invokeAndWait(() -> {});
		assertFalse(panel[0].isAccountLoaded());
	}
	@Test public void refreshWhileLoadingStartsNewRequestAndDiscardsLateCatalog() throws Exception {
		FakeApi api = new FakeApi();
		ProfilePanel[] panel = new ProfilePanel[1];
		AtomicReference<Consumer<PointsResponse>> oldPoints = new AtomicReference<>();
		AtomicReference<Consumer<AccountResponse>> oldAccount = new AtomicReference<>();
		net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		org.mockito.Mockito.when(client.getAccountHash()).thenReturn(42L);
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new ProfilePanel();
			panel[0].init(api, client, null, null, null, null);
			panel[0].loadAccount(42);
			oldPoints.set(api.points); oldAccount.set(api.account);
			panel[0].refresh();
			assertEquals("Refresh must replace an invalidated pending request", 2, api.accountRequests);
			api.points.accept(catalog("Current catalog"));
			api.account.accept(account("Current", "New rank"));
		});
		SwingUtilities.invokeAndWait(() -> {});
		oldPoints.get().accept(catalog("Stale catalog"));
		oldAccount.get().accept(account("Previous", "Old rank"));
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals("New rank", panel[0].getClanRank());
		java.lang.reflect.Field field = ProfilePanel.class.getDeclaredField("pointsData");
		field.setAccessible(true);
		assertEquals("Current catalog", ((PointsResponse.PointsData) field.get(panel[0])).getRanks().get(0).getDisplayName());
		SwingUtilities.invokeAndWait(panel[0]::onLoggedOut);
		assertNull(field.get(panel[0]));
		oldPoints.get().accept(catalog("Stale catalog"));
		SwingUtilities.invokeAndWait(() -> {});
		assertNull(field.get(panel[0]));
	}

	private static PointsResponse catalog(String name) {
		return new Gson().fromJson("{\"status\":\"success\",\"data\":{\"ranks\":[{\"name\":\"bronze\",\"displayName\":\""
			+ name + "\",\"pointsRequired\":0}]}}", PointsResponse.class);
	}

	private static AccountResponse account(String name, String rank) {
		return new Gson().fromJson("{\"status\":\"success\",\"data\":{\"osrsAccount\":{\"osrsNickname\":\""
			+ name + "\",\"clanRank\":\"" + rank + "\"},\"pointsLog\":[],\"combatAchievementPoints\":0,\"collectionLogUniqueObtained\":0}}", AccountResponse.class);
	}
	private void checkLoad(java.util.function.ObjLongConsumer<ProfilePanel> load) throws Exception {
		FakeApi api = new FakeApi();
		SwingUtilities.invokeAndWait(() -> {
			ProfilePanel panel = new ProfilePanel();
			panel.init(api, null, null, null, null, null);
			assertEquals(0, api.pointsRequests);
			assertNull(api.account);
			load.accept(panel, 42);
			assertEquals(1, api.pointsRequests);
			assertNotNull(api.account);
		});
	}
	private static class FakeApi extends RevalApiService {
		int pointsRequests;
		int accountRequests;
		Consumer<AccountResponse> account;
		Consumer<PointsResponse> points;
		Consumer<Exception> error;
		FakeApi() { super(null, new Gson()); }
		@Override public void fetchAccount(long hash, Consumer<AccountResponse> ok, Consumer<Exception> err) { accountRequests++; account = ok; error = err; }
		@Override public void fetchAccountById(int id, Consumer<AccountResponse> ok, Consumer<Exception> err) { account = ok; error = err; }
		@Override public void fetchPoints(Consumer<PointsResponse> ok, Consumer<Exception> err) { pointsRequests++; points = ok; }
	}
}
