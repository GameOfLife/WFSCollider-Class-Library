WFSPathTransformerDef( \reverse, 
		{ |f, path|
		if( f.get( \reverse ) ) {
			path.positions = path.positions.reverse;
			path.times = path.times.reverse;
			path;
		} {
			path;
		};
	},
	[ \reverse, false ],
	{ |f, path| [ \reverse, nil ] } // never bypass
)	
	.setSpec( \reverse, BoolSpec(false ) );