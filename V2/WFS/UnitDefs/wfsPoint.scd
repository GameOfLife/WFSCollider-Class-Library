Udef(\wfsPoint,
    { |point  = #[0,0]| UOut.kr(0, point.asArray) },
    [[ \point: 0@0, WFSPointSpec( 200, 0.1 ) ]]
).category_( 'wfs_control' )
