VerticalPager
=============
Sample usage

VerticalPager pager;


public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout./*your*/, container, false);
	pager = (VerticalPager) view.findViewById(R.id.pager);

	return view;
}


public void onDestroy() {
	super.onDestroy();
		
	if (pager != null) {
		pager.recycle();
	}

}

protected void setPager(List<ImageView> rects) {
	pager.removeAllViewsInLayout();
		
	for (int i = 0; i < rects.size(); i++) {
		final ImageView rect = rects.get(i);
		pager.addView(rpiv);
	}
	pager.setVisibility(View.VISIBLE);//if need
}

