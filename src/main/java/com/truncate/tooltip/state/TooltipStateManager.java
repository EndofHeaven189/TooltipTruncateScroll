package com.truncate.tooltip.state;

import java.util.Objects;

/** 所有访问都来自客户端主线程；单例中不持有游戏世界。 */
public final class TooltipStateManager {
    private static final TooltipStateManager INSTANCE = new TooltipStateManager();
    private Object screenIdentity;
    private Object targetIdentity;
    private Object contentIdentity;
    private int totalLines;
    private int pageSize = 8;
    private int scrollOffset;
    private boolean lineLimited;
    private boolean active;
    private boolean seenThisFrame;
    private double wheelRemainder;

    private TooltipStateManager() {}

    public static TooltipStateManager getInstance() {
        return INSTANCE;
    }

    public void beginFrame(Object screen) {
        if (active && screenIdentity != screen) reset();
        seenThisFrame = false;
    }

    /** 整帧没有出现 tooltip 时立即清空，避免重新悬停继承旧页码。 */
    public void endFrame() {
        if (!seenThisFrame) reset();
    }

    public void update(Object screen, Object target, Object content,
                       int lines, int configuredPageSize, boolean limited) {
        int newSize = Math.max(1, configuredPageSize);
        if (!active || screenIdentity != screen || targetIdentity != target
                || !Objects.equals(contentIdentity, content) || pageSize != newSize
                || lineLimited != limited) {
            reset();
        }
        screenIdentity = screen;
        targetIdentity = target;
        contentIdentity = content;
        totalLines = Math.max(0, lines);
        pageSize = newSize;
        lineLimited = limited;
        active = totalLines > 0;
        seenThisFrame = true;
        scrollOffset = limited ? Math.min(scrollOffset, maxOffset()) : 0;
    }

    public boolean matches(Object screen, Object target) {
        return active && screenIdentity == screen && targetIdentity == target;
    }

    public boolean canNavigate() {
        return active && lineLimited && totalLines > pageSize;
    }

    public void resetPosition() {
        scrollOffset = 0;
        wheelRemainder = 0;
    }

    public void reset() {
        screenIdentity = null;
        targetIdentity = null;
        contentIdentity = null;
        totalLines = 0;
        pageSize = 8;
        lineLimited = false;
        active = false;
        seenThisFrame = false;
        resetPosition();
    }

    /** 累积触控板的小数刻度；换方向时不保留反方向的余量。 */
    public void scroll(double wheelDelta, int speed) {
        if (!canNavigate() || !Double.isFinite(wheelDelta) || wheelDelta == 0) return;
        if (Math.signum(wheelRemainder) != Math.signum(wheelDelta)) wheelRemainder = 0;
        wheelRemainder += wheelDelta;
        double wholeSteps = wheelRemainder > 0 ? Math.floor(wheelRemainder) : Math.ceil(wheelRemainder);
        wheelRemainder -= wholeSteps;
        // 正方向滚轮表示向上；使用 double 钳制，避免异常大输入导致整数溢出。
        double next = scrollOffset - wholeSteps * Math.max(1, speed);
        scrollOffset = (int) Math.clamp(next, 0.0, (double) maxOffset());
    }

    public void flipPage(int direction) {
        if (!canNavigate()) return;
        int pageIndex = scrollOffset / pageSize;
        int nextIndex = Math.clamp(pageIndex + Integer.signum(direction), 0, getTotalPages() - 1);
        scrollOffset = nextIndex * pageSize;
        wheelRemainder = 0;
    }

    private int maxOffset() {
        // 允许最后一页不足一整页；否则 9 行、每页 8 行时无法得到第 2 页。
        return totalLines == 0 ? 0 : ((totalLines - 1) / pageSize) * pageSize;
    }

    public int getTotalLines() { return totalLines; }
    public int getScrollOffset() { return scrollOffset; }
    public int getPageSize() { return pageSize; }
    public int getCurrentPage() { return scrollOffset / pageSize + 1; }
    public int getTotalPages() { return totalLines == 0 ? 1 : (totalLines - 1) / pageSize + 1; }
    public int getVisibleEnd() { return lineLimited ? Math.min(totalLines, scrollOffset + pageSize) : totalLines; }
    public boolean hasHiddenAfter() { return getVisibleEnd() < totalLines; }
}
