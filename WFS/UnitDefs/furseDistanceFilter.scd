/*
\furseDistanceFilter

The furseDistanceFilter is a filter that applies distance filtering according to a formula by Richard W. Furse (http://www.muse.demon.co.uk/vspace/model.html): 

cutoff = 100000 / distance

where cutoff is Hz and distance is in metres.

pointFromBus: (bool) set to true if you want to set the point with a wfs_control Unit further up the chain. The settings of point in this Unit will then be neglected.
point: a Point from which the distance to the center of the room (0,0) is determined
amount: strength of the effect. A cubed multiplier for the distance. 0 means no filtering, 1 means normal, 2 means the distance in the formula is multiplied by 8 (2**3).

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

Udef( \furseDistanceFilter, { 
	var sig, point = (0@0), pointFromBus = 0, freq;
	pointFromBus = \pointFromBus.kr( pointFromBus );
	point = (\point.kr( point.asArray ) * (1-pointFromBus)) 
		+ ( UIn.kr(0,2) * pointFromBus ).asPoint;
	sig = UIn.ar(0);
	freq = (100000 / ( point.rho * \amount.kr(1).cubed )).clip(0,100000);
	sig = OnePole.ar( sig, ( -2pi * (freq / SampleRate.ir) ).exp );
	UOut.ar( 0, sig );
}).category_( \filter )
	.setSpec( \amount, [0,2,\lin,0,1].asSpec )
	.setSpec( \point, WFSPointSpec(200) )
	.setSpec( \pointFromBus, BoolSpec(false), private: true );