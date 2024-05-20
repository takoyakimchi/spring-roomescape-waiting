package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import roomescape.domain.member.Member;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationTime;
import roomescape.global.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.service.dto.FindReservationWithRankDto;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Sql(scripts = "/truncate.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class ReservationServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private MemberRepository memberRepository;

    private final String rawDate = "2060-01-01";

    private final Long timeId = 1L;
    private final Long themeId = 1L;
    private final Long memberId = 1L;

    @BeforeEach
    void setUpData() {
        jdbcTemplate.update("""
            INSERT INTO member(name, email, password, role)
            VALUES ('러너덕', 'user@a.com', '123a!', 'USER'),
                   ('트레', 'tre@a.com', '123a!', 'USER');
                        
            INSERT INTO theme(name, description, thumbnail)
            VALUES ('테마1', 'd1', 'https://test.com/test1.jpg');
                        
            INSERT INTO reservation_time(start_at)
            VALUES ('08:00');
            """);
    }

    @DisplayName("성공: 예약을 저장하고, 해당 예약을 id값과 함께 반환한다.")
    @Test
    void save() {
        Reservation saved = reservationService.reserve(memberId, rawDate, timeId, themeId);
        assertThat(saved.getId()).isEqualTo(1L);
    }

    @DisplayName("실패: 존재하지 않는 멤버 ID 입력 시 예외가 발생한다.")
    @Test
    void save_MemberIdDoesntExist() {
        assertThatThrownBy(
            () -> reservationService.reserve(2L, rawDate, timeId, themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("입력한 사용자 ID에 해당하는 데이터가 존재하지 않습니다.");
    }

    @DisplayName("실패: 존재하지 않는 날짜 입력 시 예외가 발생한다.")
    @ParameterizedTest
    @ValueSource(strings = {"2030-13-01", "2030-12-32"})
    void save_IllegalDate(String invalidRawDate) {
        assertThatThrownBy(
            () -> reservationService.reserve(memberId, invalidRawDate, timeId, themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("잘못된 날짜 형식입니다.");
    }

    @DisplayName("실패: 존재하지 않는 시간 ID 입력 시 예외가 발생한다.")
    @Test
    void save_TimeIdDoesntExist() {
        assertThatThrownBy(
            () -> reservationService.reserve(memberId, rawDate, 2L, themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("입력한 시간 ID에 해당하는 데이터가 존재하지 않습니다.");
    }

    @DisplayName("실패: 중복 예약을 생성하면 예외가 발생한다.")
    @Test
    void save_Duplication() {
        reservationService.reserve(memberId, rawDate, timeId, themeId);

        assertThatThrownBy(
            () -> reservationService.reserve(memberId, rawDate, timeId, themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("해당 시간에 예약이 이미 존재합니다.");
    }

    @DisplayName("실패: 과거 날짜 예약 생성하면 예외 발생 -- 어제")
    @Test
    void save_PastDateReservation() {
        String yesterday = LocalDate.now().minusDays(1).toString();

        assertThatThrownBy(
            () -> reservationService.reserve(memberId, yesterday, timeId, themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("과거 예약을 추가할 수 없습니다.");
    }

    @DisplayName("실패: 같은 날짜, 과거 시간 예약 생성하면 예외 발생 -- 1분 전")
    @Test
    void save_TodayPastTimeReservation() {
        String today = LocalDate.now().toString();
        String oneMinuteAgo = LocalTime.now().minusMinutes(1).toString();

        ReservationTime savedTime = reservationTimeRepository.save(new ReservationTime(oneMinuteAgo));

        assertThatThrownBy(
            () -> reservationService.reserve(memberId, today, savedTime.getId(), themeId)
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("과거 예약을 추가할 수 없습니다.");
    }

    @DisplayName("성공: 예약 대기")
    @Test
    void standby() {
        Reservation reservation = reservationService.standby(memberId, rawDate, timeId, themeId);
        assertThat(reservation.getId()).isEqualTo(1L);
    }

    @DisplayName("실패: 본인의 예약에 대기를 걸 수 없다.")
    @Test
    void standby_CantReserveAndThenStandbyForTheSameReservation() {
        reservationService.reserve(memberId, rawDate, timeId, themeId);

        assertThatThrownBy(() -> reservationService.standby(memberId, rawDate, timeId, themeId))
            .isInstanceOf(RoomescapeException.class)
            .hasMessage("이미 예약하셨습니다. 대기 없이 이용 가능합니다.");
    }

    @DisplayName("성공: 본인의 예약대기를 삭제할 수 있다.")
    @Test
    void deleteStandby() {
        reservationService.reserve(memberId, rawDate, timeId, themeId);
        reservationService.standby(2L, rawDate, timeId, themeId);
        Member member = memberRepository.findById(2L).get();

        assertThatCode(() -> reservationService.deleteStandby(2L, member))
            .doesNotThrowAnyException();
    }

    @DisplayName("실패: 타인의 예약대기를 삭제할 수 없다.")
    @Test
    void deleteStandby_ReservedByOther() {
        reservationService.reserve(memberId, rawDate, timeId, themeId);
        reservationService.standby(2L, rawDate, timeId, themeId);
        Member member = memberRepository.findById(1L).get();

        assertThatThrownBy(() -> reservationService.deleteStandby(2L, member))
            .isInstanceOf(RoomescapeException.class)
            .hasMessage("자신의 예약만 삭제할 수 있습니다.");
    }

    @DisplayName("성공: 주어진 멤버가 예약한 예약 목록 조회")
    @Test
    void findMyReservations() {
        reservationService.reserve(memberId, "2060-01-01", timeId, themeId);
        reservationService.reserve(memberId, "2060-01-02", timeId, themeId);
        reservationService.reserve(memberId, "2060-01-03", timeId, themeId);

        List<FindReservationWithRankDto> reservations = reservationService.findAllWithRankByMemberId(1L);
        assertThat(reservations).hasSize(3);
    }
}
