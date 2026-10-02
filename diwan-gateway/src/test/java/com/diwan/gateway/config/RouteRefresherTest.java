package com.diwan.gateway.config;

import com.diwan.gateway.entity.ServiceRoute;
import com.diwan.gateway.repository.ServiceRouteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteRefresherTest {

    private static ServiceRoute route(String name, String url) {
        ServiceRoute r = new ServiceRoute();
        r.setServiceName(name);
        r.setBaseUrl(url);
        r.setPathPrefix("/api/" + name);
        return r;
    }

    private final ServiceRouteRepository repo = mock(ServiceRouteRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final RouteRefresher refresher = new RouteRefresher(repo, publisher);

    @Test
    void fingerprintIgnoresOrderButNoticesEveryRelevantChange() {
        ServiceRoute a = route("a", "http://a:8080");
        ServiceRoute b = route("b", "http://b:8080");
        assertEquals(RouteRefresher.fingerprint(List.of(a, b)), RouteRefresher.fingerprint(List.of(b, a)));

        int before = RouteRefresher.fingerprint(List.of(a, b));
        b.setBaseUrl("http://b2:8080");
        assertNotEquals(before, RouteRefresher.fingerprint(List.of(a, b)));
        b.setBaseUrl("http://b:8080");
        b.setActive(false);
        assertNotEquals(before, RouteRefresher.fingerprint(List.of(a, b)));
        b.setActive(true);
        b.setTimeoutMs(1234);
        assertNotEquals(before, RouteRefresher.fingerprint(List.of(a, b)));
    }

    @Test
    void firstCheckOnlyRemembersTheStateThenReloadsOnlyWhenSomethingChanged() {
        ServiceRoute a = route("a", "http://a:8080");
        when(repo.findAll()).thenReturn(List.of(a));

        refresher.refreshIfChanged();          // baseline, no event
        refresher.refreshIfChanged();          // unchanged, no event
        verify(publisher, never()).publishEvent(any(RefreshRoutesEvent.class));

        a.setBaseUrl("http://other:8080");     // edited in the DB by someone else / another instance
        refresher.refreshIfChanged();
        verify(publisher, times(1)).publishEvent(any(RefreshRoutesEvent.class));

        refresher.refreshIfChanged();          // already reloaded
        verify(publisher, times(1)).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void aChangeMadeByThisInstanceDoesNotTriggerASecondReload() {
        ServiceRoute a = route("a", "http://a:8080");
        when(repo.findAll()).thenReturn(List.of(a));
        refresher.refreshIfChanged();

        a.setBaseUrl("http://other:8080");
        refresher.markCurrent();               // RouteService already published the event itself
        refresher.refreshIfChanged();
        verify(publisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void databaseErrorsAreSwallowed() {
        when(repo.findAll()).thenThrow(new IllegalStateException("db down"));
        refresher.refreshIfChanged();
        verify(publisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }
}
