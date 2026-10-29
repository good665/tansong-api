package kr.tansong.demo.common;

/** 페이지 번호/크기 입력값 보정 */
public final class Paging {

    private Paging() {}

    public static int page(int page) {
        return Math.max(0, Math.min(page, 10_000));
    }

    public static int size(int size) {
        return size < 1 ? 10 : Math.min(size, 50);
    }
}
