package li.cil.tis3d.common.module.execution.compiler;

import net.minecraft.network.chat.Component;

/**
 * Thrown by the {@link Compiler} when the specified code contains errors.
 */
public final class ParseException extends Exception {
    private final Component message;
    private final int lineNumber;
    private final int start;
    private final int end;

    public ParseException(final Component message, final int lineNumber, final int start, final int end) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.start = start;
        this.end = end;
    }

    /**
     * The potentially localized message that can be displayed to the user.
     *
     * @return the error message.
     */
    public Component getDisplayMessage() {
        return message;
    }

    /**
     * The line on which the error occurred.
     *
     * @return the line of the error.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * The start column (nth character) on which the error occurred.
     *
     * @return the start column of the error.
     */
    public int getStart() {
        return start;
    }

    /**
     * The end column (nth character) on which the error occurred.
     *
     * @return the end column of the error.
     */
    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return (lineNumber + 1) + ":" + (start + 1) + "-" + (end + 1) + ": " + getMessage();
    }
}
