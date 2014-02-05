/*
\wfsPathPlayer

A player for spatial trajectories. The WFSPathGUI can be called up via the [edit] button in the UChain window.

This signal can be used by the wfsDynamicPoint and wfsDynamicPlane udefs, by checking their 'pointFromBus' argument.

wfsPath: a WFSPathBuffer object. This object creates and points to a buffer with the data of the trajectory. This buffer is either filled with data sent directly, or data read from a .wfspath file (generated via the [write data] button in the GUI). Data from file is usually more reliable than sending via network.

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

Udef( \wfsPathPlayer, {
	var sig;
	sig = WFSPathBufferPlayer.kr( \wfsPath );
	UMixOut.kr( 0, sig );
}, [ [ \wfsPath, nil, WFSPathSpec() ] ] )
	.category_( 'wfs_control (deprecated)' )
	.setControlOutName( [0,1], ['x', 'y'] );