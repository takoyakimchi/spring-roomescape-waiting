package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import roomescape.domain.theme.Theme;
import roomescape.global.exception.RoomescapeException;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Sql(scripts = "/data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/truncate.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class ThemeServiceTest {

    @Autowired
    private ThemeService themeService;

    @DisplayName("실패: 이름이 동일한 방탈출 테마를 저장하면 예외 발생")
    @Test
    void save_DuplicatedName() {
        assertThatThrownBy(
            () -> themeService.save("theme1", "d", "https://d")
        ).isInstanceOf(RoomescapeException.class)
            .hasMessage("같은 이름의 테마가 이미 존재합니다.");
    }

    @DisplayName("실패: 예약에 사용되는 테마 삭제 시도 시 예외 발생")
    @Test
    void delete_ReservationExists() {
        assertThatThrownBy(() -> themeService.delete(1L))
            .isInstanceOf(RoomescapeException.class)
            .hasMessage("해당 테마를 사용하는 예약이 존재하여 삭제할 수 없습니다.");
    }

    @DisplayName("성공: 테마 추가")
    @Test
    void save() {
        Theme save = themeService.save("theme4", "desc4", "https://a.com/a.jpg");
        assertThat(save.getId()).isEqualTo(4L);
    }

    @DisplayName("성공: 테마 삭제")
    @Test
    void delete() {
        themeService.delete(2L);
        List<Theme> themes = themeService.findAll();
        assertThat(themes).hasSize(2);
    }

    @DisplayName("성공: 전체 테마 조회")
    @Test
    void findAll() {
        List<Theme> themes = themeService.findAll();
        assertThat(themes).hasSize(3);
    }

    @DisplayName("성공: 인기 테마 조회")
    @Test
    void findPopular() {
        List<Theme> themes = themeService.findPopular();
        assertThat(themes).extracting(Theme::getId).containsExactly(1L, 3L, 2L);
    }
}
