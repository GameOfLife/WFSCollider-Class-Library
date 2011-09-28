Udef( \wfsPathPlayer, {
	var input, sig;
	input = UIn.kr( 0, 2 ) * \mix_in.kr(0);
	sig = WFSPathBufferPlayer.kr( \wfsPath );
	UOut.kr( 0, sig + input );
}, [ [ \wfsPath, nil, WFSPathSpec() ] ] )
	.category_( \wfs );