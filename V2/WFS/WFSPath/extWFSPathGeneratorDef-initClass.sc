+ WFSPathGeneratorDef {
	
	// define a bunch of default path generators
	
	*initClass {
		
		WFSPathGeneratorDef(
			\line,
			{ |f, path, n| 
				var start, end;
				start = f.get( \start );
				end = f.get( \end );
				path.positions = n.collect({ |i|
					(i@i).linlin(0, n-1, start, end );
				});
				path;
			},
			[ \start, 0@0, \end, 5@5 ]
		).changesT_( false );
		
		WFSPathGeneratorDef(
			\circle,
			{ |f, path, n| 
				var center, radius, periods;
				center = f.get( \center );
				radius = f.get( \radius );
				periods = f.get( \periods );
				path.positions = n.collect({ |i|
					((i.linlin(0,n-1,0,2pi * periods) + [0,0.5pi])
						.sin.asPoint * radius) + center
				});
				path;
			},
			[ \periods, 1, \center, 0@0, \radius, 10@10 ]
		).changesT_( false );
		
		WFSPathGeneratorDef(
			\random,
			{ |f, path, n| 
				var min, max;
				min = f.get( \min );
				max = f.get( \max );
				thisThread.randSeed = f.get( \seed );
				path.positions = n.collect({ 
					(min rrand: max) @ (min rrand: max);
				});
				path;
			},
			[ \min, -8 @ -8, \max, 8 @ 8, \seed, 12345 ]
		)
			.changesT_( false )
			.setSpec( \seed, PositiveIntegerSpec(12345) );
		

	}
}