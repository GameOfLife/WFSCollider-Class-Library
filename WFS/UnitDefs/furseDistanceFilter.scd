// a distance filter according to Richard W. Furse
// http://www.muse.demon.co.uk/vspace/model.html

Udef( \furseDistanceFilter, { 
	var sig, point = (0@0), pointFromBus = 0, freq;
	pointFromBus = \pointFromBus.kr( pointFromBus );
	point = (\point.kr( point.asArray ) * (1-pointFromBus)) 
		+ ( UIn.kr(0,2) * pointFromBus ).asPoint;
	sig = UIn.ar(0);
	freq = 100000 / point.rho;
	sig = OnePole.ar( sig, ( -2pi * (freq / SampleRate.ir) ).exp * \amount.kr(1) );
	UOut.ar( 0, sig );
}).category_( \filter )
	.setSpec( \amount, [0,1,\lin,0,1].asSpec )
	.setSpec( \point, WFSPointSpec(200) )
	.setSpec( \pointFromBus, BoolSpec(false) );