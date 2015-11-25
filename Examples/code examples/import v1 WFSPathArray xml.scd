(
Dialog.openPanel({ |file|
	var array;
	array = WFSPathArray.readWFSFile( file );
	UScore( *array.collect({ |path, i|
		UChain( 0, i, path.length, \bufSoundFile, [ \wfsDynamicPoint, [ 
				\point, UMap( \trajectory, [ 
					\trajectory, WFSPathBuffer( path.asWFSPath2 ) ] ),
				\distanceFilter, 1
			]
		] )
	}) ).gui;
});
)