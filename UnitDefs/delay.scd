// If the delay is played on a different output bus, the original signal will remain untouched

Udef( \delay, { |time = 0.1, maxTime = 0.1, dry = 1, amp = 0.2|
	var in, delayed;
	in = UIn.ar( 0, 1 );
	delayed = DelayC.ar( in, maxTime, time, amp );
	UOut.ar( 0, delayed + ( in * dry ) )
} )
	.category_( \effect )
	.setSpec( \time, [ 0, 2, \lin, 0, 0.1 ] )
	.setSpec( \maxTime, [ 0, 2, \lin, 0, 0.1 ] )
	.setSpec( \dry, [0,1,\lin,0] );
	