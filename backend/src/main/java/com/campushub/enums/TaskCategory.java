package com.campushub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskCategory {
    EXPRESS("EXPRESS"),
    ERRAND("ERRAND"),
    TUTORING("TUTORING"),
    SECOND_HAND("SECOND_HAND"),
    LOST_FOUND("LOST_FOUND"),
    CONSULTATION("CONSULTATION"),
    TEAM_UP("TEAM_UP"),
    OTHER("OTHER");

    @EnumValue
    private final String value;

    TaskCategory(String value) {
        this.value = value;
    }
}
