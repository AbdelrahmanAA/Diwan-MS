package com.diwan.gateway.repository;
import com.diwan.gateway.entity.ServiceRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceRouteRepository extends JpaRepository<ServiceRoute, Long> {
    List<ServiceRoute> findByActiveTrue();
    Optional<ServiceRoute> findByServiceNameAndActiveTrue(String serviceName);
    Optional<ServiceRoute> findByServiceName(String serviceName);
}
