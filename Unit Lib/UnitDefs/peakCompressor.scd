Udef( \peakCompressor, { |thresh = (-20), ratio = 1, knee = 6, att = 0, rel = 0.5, 
		makeUp = 0|
	var in;
	in = UIn.ar(0,1);
	in = SoftKneeCompressor.ar( in, in, thresh, ratio, knee, att, rel, makeUp, 0 );
	UOut.ar( 0, in );
})
	.category_( \dynamics )
	.setSpec( \thresh, [ -96, 0, \lin, 1, -20 ].asSpec )
	.setSpec( \ratio, [ 0, 1, \lin, 0, 1 ].asSpec )
	.setSpec( \knee, [ 0, 24, \lin, 1, 6 ].asSpec )
	.setSpec( \att, [ 0, 1, \lin, 0 ].asSpec )
	.setSpec( \rel, [ 0.01, 1, \exp, 0 ].asSpec )
	.setSpec( \makeUp, [ 0, 1, \lin, 0 ].asSpec );