/*
\wfsPoint

a Udef generating a single spatial position. Use this if you want to change your point live, and feed the location to multiple units in your chain (such as \furseDistanceFilter or reverbs).

This signal can be used by the wfsDynamicPoint and wfsDynamicPlane udefs, by checking their 'pointFromBus' argument.

point: a Point object, specifying the position produced by this udef
lag: a smoothing time for changes in the position. This uses a second-order filter, which may cause slight oscillations at beginnings and endings of movements. Changing the lag time during playback may cause audible artifacts.

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

Udef(\wfsPoint,
    { |point = #[0,0], lag = 0| 
        point = LPFLag.kr( point, lag );
        UOut.kr(0, point) 
}, [
    [ \point: 0@0, WFSPointSpec( 200, 0.1 ) ],
    [ \lag: 0, [ 0, 1, \lin, 0, 0].asSpec ]
]
).category_( 'wfs_control' )
.setControlOutName( [0,1], ['x', 'y'] );
