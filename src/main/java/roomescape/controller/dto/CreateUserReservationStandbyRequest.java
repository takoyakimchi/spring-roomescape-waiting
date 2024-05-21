package roomescape.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record CreateUserReservationStandbyRequest(
    @NotNull(message = "null일 수 없습니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    @NotNull(message = "null일 수 없습니다.")
    @Positive(message = "양수만 입력할 수 있습니다.")
    Long themeId,

    @NotNull(message = "null일 수 없습니다.")
    @Positive(message = "양수만 입력할 수 있습니다.")
    Long timeId
) { }
