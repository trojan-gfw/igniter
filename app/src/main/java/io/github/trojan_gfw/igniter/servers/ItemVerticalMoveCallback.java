package io.github.trojan_gfw.igniter.servers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


public class ItemVerticalMoveCallback extends ItemTouchHelper.Callback {
    private final ItemTouchHelperContract mItemTouchHelperContract;

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    public ItemVerticalMoveCallback(ItemTouchHelperContract itemTouchHelperContract) {
        mItemTouchHelperContract = itemTouchHelperContract;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int srcPos = viewHolder.getBindingAdapterPosition();
        int destPos = target.getBindingAdapterPosition();
        if (srcPos == RecyclerView.NO_POSITION || destPos == RecyclerView.NO_POSITION) {
            return false;
        }
        mItemTouchHelperContract.onRowMove(srcPos, destPos);
        return true;
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            mItemTouchHelperContract.onRowSelected(viewHolder);
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mItemTouchHelperContract.onRowClear(viewHolder);
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    public interface ItemTouchHelperContract {
        void onRowMove(int srcPosition, int destPosition);

        void onRowSelected(RecyclerView.ViewHolder viewHolder);

        void onRowClear(RecyclerView.ViewHolder viewHolder);
    }
}
