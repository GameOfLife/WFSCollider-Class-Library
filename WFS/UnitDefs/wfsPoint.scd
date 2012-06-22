// a Udef generating a single spatial position
// use this if you want to change your point live, and feed the location 
// to other units (such as \furseDistanceFilter or reverbs) as well

Udef(\wfsPoint,
    { |point = #[0,0], lag = 0| 
        point = LPFLag.kr( point, lag );
        UOut.kr(0, point) 
}, [
    [ \point: 0@0, WFSPointSpec( 200, 0.1 ) ],
    [ \lag: 0, [ 0, 1, \lin, 0, 0].asSpec ]
]
).category_( 'wfs_control' )
