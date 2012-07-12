Udef( \wfsPathPlayer, {
	var sig;
	sig = WFSPathBufferPlayer.kr( \wfsPath );
	UMixOut.kr( 0, sig );
}, [ [ \wfsPath, nil, WFSPathSpec() ] ] )
	.category_( \wfs_control )
	.setControlOutName( [0,1], ['x', 'y'] );