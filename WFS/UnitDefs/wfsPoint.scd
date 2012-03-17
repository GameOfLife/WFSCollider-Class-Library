Udef(\wfsPoint,
    { |point = #[0,0], lag = 0| 
        point = LPFLag.kr( point, lag );
        UOut.kr(0, point) 
}, [
    [ \point: 0@0, WFSPointSpec( 200, 0.1 ) ],
    [ \lag: 0, [ 0, 1, \lin, 0, 0].asSpec ]
]
).category_( 'wfs_control' )
