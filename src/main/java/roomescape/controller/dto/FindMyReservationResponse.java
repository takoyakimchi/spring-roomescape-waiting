package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.reservation.Reservation;

public record FindMyReservationResponse(Long reservationId,
                                        String theme,
                                        LocalDate date,
                                        @JsonFormat(pattern = "HH:mm") LocalTime time,
                                        String status) {

    public static FindMyReservationResponse from(Reservation reservation) {
        return new FindMyReservationResponse(
            reservation.getId(),
            reservation.getTheme().getName(),
            reservation.getDate(),
            reservation.getTime().getStartAt(),
            reservation.getStatus().toString()
        );
    }
}