/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

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
		)	
			.changesT_( false )
			.setSpec( \start, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \end, PointSpec( 200, 0.1@0.1 ) );
		
		WFSPathGeneratorDef(
			\circle,
			{ |f, path, n| 
				var clockwise, startAngle, center, radius, periods;
				clockwise = f.get( \clockwise ).binaryValue.linlin( 0,1,-1,1);
				startAngle = (f.get( \startAngle ) / 360) * 2pi;
				center = f.get( \center );
				radius = f.get( \radius );
				periods = f.get( \periods );
				path.positions = n.collect({ |i|
					(((i.linlin(0,n-1,0,2pi * periods) * clockwise) + [0,0.5pi] + startAngle)
						.sin.asPoint * radius) + center
				});
				path;
			},
			[ \periods, 1, \startAngle, 0, \clockwise, true, \center, 0@0, \radius, 10@10 ]
		)
			.changesT_( false )
			.setSpec( \periods, [0, inf, \lin, 0.125, 1].asSpec )
			.setSpec( \startAngle, [-180,180,\lin,1,0].asSpec )
			.setSpec( \center, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) );
		
		WFSPathGeneratorDef(
			\random,
			{ |f, path, n| 
				var center, radius;
				center = f.get( \center );
				radius = f.get( \radius );
				thisThread.randSeed = f.get( \seed );
				path.positions = n.collect({ 
					(Polar( 1.0.rand, 2pi.rand ).asPoint * radius ) + center;
				});
				path;
			},
			[ \center, 0@0, \radius, 10@10, \seed, 12345 ]
		)
			.changesT_( false )
			.setSpec( \seed, PositiveIntegerSpec(12345) )
			.setSpec( \center, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) );
			
		WFSPathGeneratorDef(
			\brown,
			{ |f, path, n| 
				var center, radius, minRadius, angleStep, step;
				var wx, wy;
				var wr, wa;
				center = f.get( \center );
				radius = f.get( \radius );
				angleStep = (f.get( \angleStep ) / 360) * 2pi;
				step = f.get( \step );
				thisThread.randSeed = f.get( \seed );
				wr = Pbrown( 0, 1, step ).asStream;
				wa = Pseries( 0, Pwhite( angleStep.neg, angleStep ) ).asStream;
				path.positions = n.collect({ 
					(Polar( wr.next, wa.next ).asPoint * radius) + center
				});
				path;
			},
			[  \center, 0@0, \radius, 10@10,
				\angleStep, 20, \step, 0.125, \seed, 12345 ]
		)
			.changesT_( false )
			.setSpec( \seed, PositiveIntegerSpec(12345) )
			.setSpec( \center, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) )
			.setSpec( \angleStep, [0,360].asSpec )
			.setSpec( \step, [0,1].asSpec );
			
		WFSPathGeneratorDef(
			\sine,
			{ |f, path, n| 
				var start, end, phase, amp, periods;
				var polar;
				start = f.get( \start );
				end = f.get( \end );
				periods = f.get( \periods );
				amp = f.get( \amp );
				phase = f.get( \phase ) * pi;
				polar = (end - start).asPolar;
				path.positions = n.collect({ |i|
					var sin;
					sin = (i.linlin(0,n-1,0,2pi * periods) + phase).sin * amp;
					( i.linlin(0,n-1,0,polar.rho) @ sin ).rotate( polar.theta ) + start;
				});
				path;
			},
			[ \periods, 1, \phase, 0, \start, 0@0, \end, 5@5, \amp, 2 ]
		)
			.changesT_( false )
			.setSpec( \periods, [0, inf, \lin, 0.125].asSpec )
			.setSpec( \phase, [0, 2, \lin].asSpec )
			.setSpec( \start, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \end, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \amp, [ -inf, inf, \lin, 0.125 ].asSpec );


	}
}