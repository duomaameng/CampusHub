package com.campushub.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private int page;
    private int size;
    private int pages;
    private List<T> records;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> records) {
        int pages = (int) Math.ceil((double) total / size);
        return new PageResult<>(total, page, size, pages, records);
    }
}
