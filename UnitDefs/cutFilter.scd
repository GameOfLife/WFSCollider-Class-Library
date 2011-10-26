Udef( \cutFilter, { |freq #[ 20, 20000 ], order = 2|
	var in;
	in = UIn.ar(0,1);
	in = BLowCut.ar( in, freq[0], order, 3 );
	in = BHiCut.ar( in, freq[1], order, 3 );
	UOut.ar( 0, in );
})
	.category_( \filter )
	.setSpec( \freq, \freq.asSpec.asRangeSpec.default_( [20,20000] ) )
	.setSpec( \order, [ 0, 3, \lin, 1, 2 ].asSpec )