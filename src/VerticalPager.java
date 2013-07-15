package mobi.ifunny.view.rage;

import mobi.ifunny.Debug;
import mobi.ifunny.view.drawable.RageCropDrawable;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

public class VerticalPager extends ViewGroup {
	public static final String TAG = VerticalPager.class.getSimpleName();

	public static final int INVALID_PAGE = -1;
	private static final int scrollDuration = 300;

	private int pageHeight;
	private int measuredHeight;

	private volatile int mCurrentPage;

	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mTouchSlop;
	private int mMaximumVelocity;
	private int lastScrollY;

	private float mLastMotionY;
	private float mLastMotionX;

	private final static int TOUCH_STATE_REST = 0;
	private final static int TOUCH_STATE_SCROLLING = 1;

	private int mTouchState = TOUCH_STATE_REST;

	private OnScrollingListener mListener;
	private GestureDetector mGestureDetector;

	public interface Callback {
		void onVerticalPagerSingleTap();
	}

	/**
	 * Used to inflate the Workspace from XML.
	 * 
	 * @param context The application's context.
	 * @param attrs The attribtues set containing the Workspace's customization values.
	 */
	public VerticalPager(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Used to inflate the Workspace from XML.
	 * 
	 * @param context The application's context.
	 * @param attrs The attribtues set containing the Workspace's customization values.
	 * @param defStyle Unused.
	 */
	public VerticalPager(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	/**
	 * Initializes various states for this workspace.
	 */
	private void init(Context context) {
		mCurrentPage = 0;
		mScroller = new Scroller(context, new DecelerateInterpolator());

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		setVerticalScrollBarEnabled(false);
		setVerticalFadingEdgeEnabled(true);
		setScrollbarFadingEnabled(true);

		//
		mListener = new OnScrollingListener() {
			@Override
			public void onScroll(int scrollX) {
				if (scrollX == 0) {
					return;
				}
				//
				if (Math.abs(scrollX) < pageHeight) {
					if (!mScroller.isFinished()) {
						mScroller.abortAnimation();
					}
					////
					int nextPage = getChildNumberForCurrentScrollFling(scrollX);
					snapToPage(nextPage);
					////
					Debug.logi(TAG, "OnScrollingListener::onScroll(" + scrollX + ") # snapToPage("
							+ nextPage + ");");
				}
			}
		};
		//
		mGestureDetector = new GestureDetector(context, new GestureListener());
		mGestureDetector.setIsLongpressEnabled(false);
		//
		setChildrenDrawingCacheEnabled(true);
		setChildrenDrawnWithCacheEnabled(true);
	}

	/**
	 * Returns the index of the currently displayed page.
	 * 
	 * @return The index of the currently displayed page.
	 */
	public int getCurrentPage() {
		return mCurrentPage;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
		else {
			//TODO: clearChildrenCache();
		}
	}

	@Override
	protected int computeVerticalScrollRange() {
		return measuredHeight;
	}

	@Override
	protected int computeVerticalScrollExtent() {
		return getMeasuredWidth();
	}

	@Override
	protected int computeVerticalScrollOffset() {
		int offSet = getScrollY();// - getBeginY() / 2;
		return (offSet > 0) ? offSet : 0;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {

		// ViewGroup.dispatchDraw() supports many features we don't need:
		// clip to padding, layout animation, animation listener, disappearing
		// children, etc. The following implementation attempts to fast-track
		// the drawing dispatch by drawing only what we know needs to be drawn.

		final long drawingTime = getDrawingTime();
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			drawChild(canvas, getChildAt(i), drawingTime);
		}

		if (mCurrentPage == INVALID_PAGE) {
//			if (BuildConfig.DEBUG) {
//				Log.i(TAG, "dispatchDraw::(mCurrentPage == INVALID_PAGE) # mListener.onScroll; "
//						+ mCurrentPage);
//			}
			mListener.onScroll(mScroller.getFinalY() - mScroller.getCurrY());
		}
		//
		super.onDrawScrollBars(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		pageHeight = getMeasuredWidth();//getMeasuredHeight();
		final int count = getChildCount();
		if (count == 0) {
			return;
		}
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(MeasureSpec.makeMeasureSpec(pageHeight, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(pageHeight, MeasureSpec.UNSPECIFIED));
		}
	}

	private int getBeginY() {
		return getHeight() / 2;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int count = getChildCount();

		measuredHeight = getBeginY();

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				int chHeight = child.getMeasuredHeight();
				if (i == 0) {
					measuredHeight -= chHeight / 2;
				}
				child.layout(child.getPaddingLeft(), measuredHeight, child.getMeasuredWidth()
						- child.getPaddingRight(), measuredHeight + chHeight);
				measuredHeight += chHeight;

			}
		}
	}

	/*
	private boolean canScrollX() {
		View child = getChildAt(mCurrentPage);
		if (child != null) {
			int childWidth = child.getWidth();
			return getWidth() < childWidth + getPaddingLeft() + getPaddingRight();
		}
		return false;
	}
	*/
	/*
	public boolean canScrollY(int dy) {
		View child = getChildAt(mCurrentPage);
		if (child != null) {
			if (child instanceof RageImageView) {
				RageImageView piv = (RageImageView) child;
				return piv.canScrollY(dy);
			}
		}
		return false;
	}
	*/

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
//			Log.d(TAG,
//					"onInterceptTouchEvent::((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) # return true;");
			return true;
		}

		if (!mScroller.isFinished()) {
//			Log.d(TAG, "onInterceptTouchEvent::(!mScroller.isFinished()) # return true;");
			return true;
		}

		final float y = ev.getY();
		final float x = ev.getX();

		switch (action) {
			case MotionEvent.ACTION_MOVE:
				/*
				 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
				 * whether the user has moved far enough from his original down touch.
				 */
				if (mTouchState == TOUCH_STATE_REST) {
					checkStartScroll(x, y);
				}

				break;

			case MotionEvent.ACTION_DOWN:
				// Remember location of down touch
				mLastMotionX = x;
				mLastMotionY = y;

				/*
				 * If being flinged and user touches the screen, initiate drag;
				 * otherwise don't.  mScroller.isFinished should be false when
				 * being flinged.
				 */
				mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				// Release the drag
				//TODO:  clearChildrenCache();
				mTouchState = TOUCH_STATE_REST;
				break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
//		Log.d(TAG, "onInterceptTouchEvent::(mTouchState != TOUCH_STATE_REST) # return "
//				+ (mTouchState != TOUCH_STATE_REST) + ";");
		return (mTouchState != TOUCH_STATE_REST);
	}

	private void checkStartScroll(float x, float y) {
		/*
		 * Locally do absolute value. mLastMotionX is set to the y value
		 * of the down event.
		 */
		final int xDiff = (int) Math.abs(x - mLastMotionX);
		final int yDiff = (int) Math.abs(y - mLastMotionY);

		boolean yMoved = yDiff > mTouchSlop;
		boolean xSmaller = yDiff > xDiff;

		if (yMoved && xSmaller) {
			// Scroll if the user moved far enough along the X axis
			mTouchState = TOUCH_STATE_SCROLLING;
			//TODO:  enableChildrenCache();
		}
	}

	//TODO:
//	void enableChildrenCache() {
//		setChildrenDrawingCacheEnabled(true);
//		setChildrenDrawnWithCacheEnabled(true);
//	}

	//TODO:
//	void clearChildrenCache() {
//		setChildrenDrawingCacheEnabled(false);
//		setChildrenDrawnWithCacheEnabled(false);
//	}

	private boolean firstMoveFlag;

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int count = getChildCount();
		if (count == 0) {
			return false;
		}
		//
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
		//
		if (mGestureDetector.onTouchEvent(ev)) {
			return true;
		}
		//
		final int action = ev.getAction();
		final float x = ev.getX();
		final float y = ev.getY();

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (!mScroller.isFinished()) {
					if (mCurrentPage == INVALID_PAGE) {
						mCurrentPage = getChildNumberForCurrentScrollFling(mScroller.getFinalY()
								- mScroller.getCurrY());
						mScroller.abortAnimation();
						snapToPage(mCurrentPage);
					}
				}
				// Remember where the motion event started
				mLastMotionX = x;
				mLastMotionY = y;
				//
				firstMoveFlag = true;
				break;
			case MotionEvent.ACTION_MOVE:
				if (mTouchState == TOUCH_STATE_REST) {
					checkStartScroll(x, y);
				}
				else if (mTouchState == TOUCH_STATE_SCROLLING) {
					// Scroll to follow the motion event
					int deltaY = (int) (mLastMotionY - y);
					if (firstMoveFlag) {
						deltaY = 0;
						firstMoveFlag = false;
					}
					mLastMotionX = x;
					mLastMotionY = y;

					// Apply friction to scrolling past boundaries.
					final int botomY = getChildAt(count - 1).getBottom();
					if (getScrollY() < 0 || getScrollY() + pageHeight > botomY) {
						if (Math.abs(deltaY) >= 3) deltaY /= 3;
					}

					setVerticalScrollBarEnabled(true);
					scrollBy(0, deltaY);
					awakenScrollBars();
					invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTouchState == TOUCH_STATE_SCROLLING) {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(500, mMaximumVelocity);
					int velocityY = (int) velocityTracker.getYVelocity();
					Debug.logi(TAG, "velocityY = " + velocityY + ";  mMaximumVelocity="
							+ mMaximumVelocity + ";");

					int maxTop = 0;
					int maxBottom = measuredHeight - getBeginY()
							- getChildAt(getChildCount() - 1).getHeight() / 2;
					if (getScrollY() < maxTop) {//top
						snapToPage(0);
						Debug.logi(TAG,
								"onTouchEvent::case MotionEvent.ACTION_UP:(getScrollY() < maxTop) # snapToPage(0);");
					}
					else if (getScrollY() > maxBottom) {//bottom
						if ((count - 1) >= 0) {
							snapToPage(count - 1);
						}
						Debug.logi(TAG,
								"onTouchEvent::case MotionEvent.ACTION_UP:(getScrollY() > maxBottom) # snapToPage(count - 1);");
					}
					else {
						if (Math.abs(velocityY) < getHeight()) {
							int scrollDy = getScrollY() - lastScrollY;
							mCurrentPage = getChildNumberForCurrentScrollPosition(scrollDy);

							mScroller.abortAnimation();
							snapToPage(mCurrentPage);

							Debug.logi(TAG,
									"onTouchEvent::case MotionEvent.ACTION_UP:else(Math.abs(velocityY) < "
											+ getHeight() + ") # snapToPage(" + mCurrentPage + ");");
						}
						else {
							mScroller.fling(getScrollX(), getScrollY(), 0, -velocityY, 0, 0, 0,
									maxBottom);
							mScroller.extendDuration(1000);
							//
							mCurrentPage = INVALID_PAGE;
							awakenScrollBars();
							invalidate();
							Debug.logi(TAG,
									"onTouchEvent::case MotionEvent.ACTION_UP:else(Math.abs(velocityY) > "
											+ getHeight() + ") # mScroller.fling();");
						}
					}
				}
				//
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				//
				mTouchState = TOUCH_STATE_REST;
				break;
			case MotionEvent.ACTION_CANCEL:
				mTouchState = TOUCH_STATE_REST;
		}
		return true;
	}

	private void snapToPage(final int whichPage) {
		//TODO: enableChildrenCache();

		RageImageView child = (RageImageView) getChildAt(whichPage);
		int cHeightHalf = getBeginY() - child.getFitHeight() / 2;
		int scrollY = getScrollY();
		final int deltaY = (child.getTop() - cHeightHalf) - scrollY;

		mScroller.startScroll(0, scrollY, 0, deltaY, scrollDuration);
		mCurrentPage = whichPage;
		lastScrollY = scrollY + deltaY;

		awakenScrollBars();
		invalidate();
	}

	private int getChildNumberForCurrentScrollFling(int lastPath) {
		int count = getChildCount();
		int scrollY = getScrollY();

		for (int i = 0; i < count; i++) {
			final RageImageView child = (RageImageView) getChildAt(i);

			int topY = child.getTop();
			int bottomY = child.getBottom();

			int scrollYAddHalfPage = getBeginY() + scrollY;
			scrollYAddHalfPage += (lastPath >= 0) ? pageHeight / 2 : 0;//-pageHeight / 2;

			if ((topY < scrollYAddHalfPage) && (bottomY > scrollYAddHalfPage)) {

				if (lastPath > 0) {
					return i;
				}
				else {
					return ((i - 1 >= 0) ? i - 1 : i);
				}
			}
		}
		return getChildNumberForCurrentScrollPosition(lastPath);
	}

	private int getChildNumberForCurrentScrollPosition(int lastPath) {
		int count = getChildCount();
		int scrollY = getScrollY();

		int scrollYAddHalfPage = getBeginY() + scrollY;
		int searchH = getWidth() / 5;
		if (mCurrentPage != INVALID_PAGE) {
			final RageImageView child = (RageImageView) getChildAt(mCurrentPage);
			searchH = child.getFitHeight() / 2;
		}
		scrollYAddHalfPage += (lastPath >= 0) ? searchH : -searchH;
		//scrollYAddHalfPage += (lastPath >= 0) ? getWidth() / 5 : -getWidth() / 5;
//				child.getFitHeight() / 2 : -child.getFitHeight() / 2;

		for (int i = 0; i < count; i++) {
			final RageImageView child = (RageImageView) getChildAt(i);

			int topY = child.getTop();
			int bottomY = child.getBottom();

			if ((topY < scrollYAddHalfPage) && (bottomY > scrollYAddHalfPage)) {
				return i;
			}
		}
		return (scrollY < getBeginY()) ? 0 : (count - 1);
	}

	public void recycle() {
		final int count = getChildCount();
		if (count == 0) {
			return;
		}
		//
		final RageImageView child = (RageImageView) getChildAt(0);
		RageCropDrawable drawable = (RageCropDrawable) child.getDrawable();
		if (drawable != null) drawable.recycle();
	}

	/**
	 * Implement to receive events on scroll position and page snaps.
	 */
	private interface OnScrollingListener {
		/**
		 * Receives the current scroll X value. This value will be adjusted to assume the left edge of the first page
		 * has a scroll position of 0. Note that values less than 0 and greater than the right edge of the last page are
		 * possible due to touch events scrolling beyond the edges.
		 * 
		 * @param scrollX Scroll X value
		 */
		void onScroll(int scrollX);

//		/**
//		 * Invoked when scrolling is finished (settled on a page, centered).
//		 *
//		 * @param currentPage The current page
//		 */
//		void onViewScrollFinished(int currentPage);
	}

	private class GestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			Context context = getContext();
			if (context instanceof Callback) {
				Callback callback = (Callback) context;
				callback.onVerticalPagerSingleTap();
				return true;
			}
			return false;
		}
	}

}
