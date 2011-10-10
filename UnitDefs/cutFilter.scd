Udef( \cutFilter, { |lowCut = 20, highCut = 20000, order = 2|
	var in;
	in = UIn.ar(0,1);
	in = BLowCut.ar( in, lowCut, order, 3 );
	in = BHiCut.ar( in, highCut, order, 3 );
	UOut.ar( 0, in );
})
	.category_( \filter )
	.setSpec( \lowCut, \freq.asSpec )
	.setSpec( \highCut, \freq.asSpec )
	.setSpec( \order, [ 0, 3, \lin, 1, 2 ].asSpec )