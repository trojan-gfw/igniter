package io.github.trojan_gfw.igniter;

import android.text.Editable;
import android.text.TextWatcher;


/**
 * Text view listener which splits the update text event in four parts:
 * <ul>
 *     <li>The text placed <b>before</b> the updated part.</li>
 *     <li>The <b>old</b> text in the updated part.</li>
 *     <li>The <b>new</b> text in the updated part.</li>
 *     <li>The text placed <b>after</b> the updated part.</li>
 * </ul>
 * <code>
 *     myEditText.addTextChangedListener(new TextViewListener() {
 *         \@Override
 *         protected void onTextChanged(String before, String old, String aNew, String after) {
 *            // intuitive usation of parametters
 *            String completeOldText = before + old + after;
 *            String completeNewText = before + aNew + after;
 *
 *            // update TextView
 *             startUpdates(); // to prevent infinite loop.
 *             myEditText.setText(myNewText);
 *             endUpdates();
 *         }
 * }
 * </code>
 * Created by Jeremy B.
 */

public abstract class TextViewListener implements TextWatcher {
    /**
     * Unchanged sequence which is placed before the updated sequence.
     */
    private String _before;

    /**
     * Updated sequence before the update.
     */
    private String _old;

    /**
     * Updated sequence after the update.
     */
    private String _new;

    /**
     * Unchanged sequence which is placed after the updated sequence.
     */
    private String _after;

    /**
     * Indicates when changes are made from within the listener, should be omitted.
     */
    private boolean _ignore = false;

    @Override
    public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
        _before = sequence.subSequence(0,start).toString();
        _old = sequence.subSequence(start, start+count).toString();
        _after = sequence.subSequence(start+count, sequence.length()).toString();
    }

    @Override
    public void onTextChanged(CharSequence sequence, int start, int before, int count) {
        _new = sequence.subSequence(start, start+count).toString();
    }

    @Override
    public void afterTextChanged(Editable sequence) {
        if (_ignore)
            return;

        onTextChanged(_before, _old, _new, _after);
    }

    /**
     * Triggered method when the text in the text view has changed.
     * <br/>
     * You can apply changes to the text view from this method
     * with the condition to call {@link #startUpdates()} before any update,
     * and to call {@link #endUpdates()} after them.
     *
     * @param before Unchanged part of the text placed before the updated part.
     * @param old Old updated part of the text.
     * @param aNew New updated part of the text?
     * @param after Unchanged part of the text placed after the updated part.
     */
    protected abstract void onTextChanged(String before, String old, String aNew, String after);

    /**
     * Call this method when you start to update the text view, so it stops listening to it and then prevent an infinite loop.
     * @see #endUpdates()
     */
    protected void startUpdates(){
        _ignore = true;
    }

    /**
     * Call this method when you finished to update the text view in order to restart to listen to it.
     * @see #startUpdates()
     */
    protected void endUpdates(){
        _ignore = false;
    }
}
