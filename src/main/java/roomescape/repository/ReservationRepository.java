package roomescape.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import roomescape.domain.reservation.Date;
import roomescape.domain.reservation.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Boolean existsByTimeId(Long id);

    Boolean existsByThemeId(Long id);

    Boolean existsByDateAndTimeIdAndThemeId(Date date, Long timeId, Long themeId);

    List<Reservation> findAllByDateAndThemeId(Date date, Long themeId);

    List<Reservation> findAllByThemeIdAndMemberIdAndDateIsBetween(Long themeId, Long memberId, Date dateFrom,
        Date dateTo);

    List<Reservation> findAllByMemberId(Long memberId);
}
