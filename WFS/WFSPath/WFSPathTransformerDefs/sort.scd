WFSPathTransformerDef( \sort, 
	{ |f, path| 
		var sortedPos, oldPos, type;
		type = f.get( \sort );
		switch( type,
			\distance, {
				sortedPos = path.positions.deepCopy.sort({ |a,b|
					a.rho <= b.rho;
				});
			},
			\angle, {
				sortedPos = path.positions.deepCopy.sort({ |a,b|
					a.theta <= b.theta;
				});
			},
			\nearest, {
				oldPos = path.positions.reverse;
				sortedPos = Array( oldPos.size );
				sortedPos.add( oldPos.pop );
				oldPos.size.do({ 
					oldPos.sort({ |a,b|
						a.dist( sortedPos.last ) >= b.dist( sortedPos.last )
					});
					sortedPos.add( oldPos.pop );
				});
			},
			\rel_angle, {
				oldPos = path.positions.reverse;
				sortedPos = Array( oldPos.size );
				sortedPos.add( oldPos.pop );
				if( oldPos.size > 0 ) { sortedPos.add( oldPos.pop ); };
				oldPos.size.do({ 
					var lastAngle;
					lastAngle = (sortedPos.last - sortedPos[ sortedPos.size - 2 ]).angle;
					oldPos.sort({ |a,b|
						var aa, bb;
						aa = lastAngle - ((a - sortedPos.last).angle).wrap(-pi,pi);
						bb = lastAngle - ((b - sortedPos.last).angle).wrap(-pi,pi);
						if( aa.abs.round(0.01pi) == bb.abs.round(0.01pi) ) {
							a.dist( sortedPos.last ) >= b.dist( sortedPos.last )
						} {
							aa.abs >= bb.abs
						};
					});
					sortedPos.add( oldPos.pop );
				});
			},
			\x, {
				sortedPos = path.positions.deepCopy.sort({ |a,b|
					a.x <= b.x;
				});
			},
			\y, {
				sortedPos = path.positions.deepCopy.sort({ |a,b|
					a.y <= b.y;
				});
			},
			\mean_xy, {
				sortedPos = path.positions.deepCopy.sort({ |a,b|
					(a.x + a.y) <= (b.x + b.y);
				});
			}
		);
		if( sortedPos.notNil ) { path.positions_(sortedPos); };
	}, 
	[ \sort, \off ]
)
	.setSpec( \sort, ListSpec( [ \off, \distance, \angle, \nearest, \rel_angle, \x, \y, \mean_xy ] ) );