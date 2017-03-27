package com.Revsoft.Wabbitemu.wizard;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ViewAnimator;
import com.Revsoft.Wabbitemu.R;
import com.Revsoft.Wabbitemu.utils.UserActivityTracker;
import java.util.Stack;

public class WizardController {
    private final Activity mActivity;
    private final AnimationListener mAnimationListener = new AnimationListener() {
        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            WizardController.this.mActivity.setTitle(WizardController.this.mCurrentController.getTitleId());
            WizardController.this.mWizardNavController.onPageLaunched(WizardController.this.mCurrentController);
        }
    };
    private WizardPageController mCurrentController;
    private final OnWizardFinishedListener mFinishedListener;
    private final SparseIntArray mIdToPositionMap = new SparseIntArray();
    private final SparseArray<WizardPageController> mPageControllers = new SparseArray();
    private Stack<Object> mPreviousData = new Stack();
    private final UserActivityTracker mUserActivityTracker = UserActivityTracker.getInstance();
    private final ViewAnimator mViewFlipper;
    private final WizardNavigationController mWizardNavController;

    public WizardController(@NonNull Activity activity, @NonNull ViewAnimator viewFlipper, @NonNull ViewGroup navContainer, @NonNull OnWizardFinishedListener onFinishListener) {
        this.mActivity = activity;
        this.mViewFlipper = viewFlipper;
        this.mFinishedListener = onFinishListener;
        this.mWizardNavController = new WizardNavigationController(this, navContainer);
        int i = 0;
        while (i < viewFlipper.getChildCount()) {
            int pageId = viewFlipper.getChildAt(i).getId();
            if (pageId == -1) {
                throw new IllegalStateException("Child at index " + i + " missing id");
            } else if (this.mIdToPositionMap.get(pageId) != 0) {
                throw new IllegalStateException("Duplicate page id " + pageId);
            } else {
                this.mIdToPositionMap.put(pageId, i);
                i++;
            }
        }
    }

    public void registerView(int pageId, @NonNull WizardPageController pageController) {
        this.mPageControllers.put(pageId, pageController);
        if (this.mIdToPositionMap.get(pageId, -1) == -1) {
            throw new IllegalStateException("View Id must be child of the view animator");
        } else if (this.mCurrentController == null) {
            this.mCurrentController = pageController;
            showPage(pageId, null);
            this.mWizardNavController.onPageLaunched(this.mCurrentController);
        }
    }

    public boolean moveNextPage() {
        if (this.mCurrentController.isFinalPage()) {
            this.mUserActivityTracker.reportBreadCrumb("Finishing final page");
            this.mFinishedListener.onWizardFinishedListener(this.mCurrentController.getControllerData());
            return true;
        } else if (!this.mCurrentController.hasNextPage()) {
            return false;
        } else {
            setNextAnimation();
            int nextPageId = this.mCurrentController.getNextPage();
            Object previousData = this.mCurrentController.getControllerData();
            this.mPreviousData.push(previousData);
            moveToPage(nextPageId, previousData);
            return true;
        }
    }

    public boolean movePreviousPage() {
        if (!this.mCurrentController.hasPreviousPage()) {
            return false;
        }
        setBackAnimation();
        moveToPage(this.mCurrentController.getPreviousPage(), this.mPreviousData.pop());
        return true;
    }

    private void moveToPage(int nextPageId, Object data) {
        WizardPageController lastController = this.mCurrentController;
        this.mCurrentController = (WizardPageController) this.mPageControllers.get(nextPageId);
        if (this.mCurrentController == null) {
            throw new IllegalStateException("Must have registered next page");
        }
        lastController.onHiding();
        this.mViewFlipper.getInAnimation().setAnimationListener(this.mAnimationListener);
        if (this.mIdToPositionMap.get(nextPageId, -1) == -1) {
            throw new IllegalStateException("Id is not registered " + nextPageId);
        }
        this.mUserActivityTracker.reportBreadCrumb("Moving to page %s from %s", this.mCurrentController.getClass().getSimpleName(), lastController.getClass().getSimpleName());
        showPage(nextPageId, data);
    }

    private void showPage(int nextPageId, Object data) {
        this.mViewFlipper.setDisplayedChild(this.mIdToPositionMap.get(nextPageId));
        this.mCurrentController.onShowing(data);
    }

    private void setNextAnimation() {
        this.mViewFlipper.setOutAnimation(this.mActivity, R.anim.out_to_left);
        this.mViewFlipper.setInAnimation(this.mActivity, R.anim.in_from_right);
    }

    private void setBackAnimation() {
        this.mViewFlipper.setOutAnimation(this.mActivity, R.anim.out_to_right);
        this.mViewFlipper.setInAnimation(this.mActivity, R.anim.in_from_left);
    }
}
