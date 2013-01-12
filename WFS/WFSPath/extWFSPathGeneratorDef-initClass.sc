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
				var start, end, curve;
				start = f.get( \start );
				end = f.get( \end );
				curve = f.get( \curve );
				path.positions = n.collect({ |i|
					(i@i).performOnEach( \lincurve, 0, n-1, start, end, curve );
				});
				path;
			},
			[ \start, 0@0, \end, 5@5, \curve, 0@0 ]
		)	
			.changesT_( false )
			.setSpec( \start, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \end, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \curve, PointSpec( 10, 0.1@0.1 ) );
			
		
		WFSPathGeneratorDef(
			\spline,
			{ |f, path, n| 
				var start, end, c1, c2;
				var part1;
				start = f.get( \start );
				end = f.get( \end );
				c1 = f.get( \c1 );
				c2 = f.get( \c2 );
				part1 = [ start, end ].splineIntPart1( c1, c2 );
				path.positions = n.collect({ |i|
					part1.splineIntPart2( i / (n-1) )
				});
				path;
			},
			[ \start, 0@0, \end, 5@5, \c1, 0@5, \c2, 5@0 ]
		)	
			.changesT_( false )
			.setSpec( \start, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \end, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \c1, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \c2, PointSpec( 200, 0.1@0.1 ) );
		
		WFSPathGeneratorDef(
			\circle,
			{ |f, path, n| 
				var clockwise, startAngle, center, radius, periods;
				var close, nn = n;
				clockwise = f.get( \clockwise ).binaryValue.linlin( 0,1,-1,1);
				startAngle = (f.get( \startAngle ) / 360) * 2pi;
				center = f.get( \center );
				radius = f.get( \radius );
				periods = f.get( \periods );
				close = f.get( \close );
				if( close ) { nn = n-1 };
				path.positions = n.collect({ |i|
					(((i.linlin(0,nn,0,2pi * periods) * clockwise) + [0,0.5pi] + startAngle)
						.sin.asPoint * radius) + center
				});
				path;
			},
			[ \periods, 1, \close, true, \startAngle, 0, 
				\clockwise, true, \center, 0@0, \radius, 8@8 
			]
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
			[ \center, 0@0, \radius, 8@8, \seed, 12345 ]
		)
			.changesT_( false )
			.setSpec( \seed, PositiveIntegerSpec(12345) )
			.setSpec( \center, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) );
			
		WFSPathGeneratorDef(
			\randTime,
			{ |f, path, n| 
				var min, max, seed, dur;
				min = f.get( \min );
				max = f.get( \max );
				seed = f.get( \seed );
				dur = path.times.sum;
				thisThread.randSeed = f.get( \seed );
				path.times = path.times.collect({ |item|
					(item * min) exprand: (item * max);
				}).normalizeSum( dur ) * dur;
			},
			[ \min, 0.5, \max, 2, \seed, 12345 ]
		)
			.changesX_( false )
			.changesY_( false )
			.setSpec( \min, [0.01, 100, \exp, 0, 0.5].asSpec )
			.setSpec( \max, [0.01, 100, \exp, 0, 2].asSpec )
			.setSpec( \seed, PositiveIntegerSpec(12345) );
			
		WFSPathGeneratorDef(
			\lineTime,
			{ |f, path, n| 
				var start, end, curve, dur, nx;
				start = f.get( \start );
				end = f.get( \end );
				curve = f.get( \curve );
				dur = path.times.sum;
				nx = path.times.size - 1;
				path.times = path.times.collect({ |item, i|
					i.lincurve( 0, nx, start, end, curve ) * item			}).normalizeSum( dur ) * dur;
			},
			[ \start, 0.5, \end, 2, \curve, 0 ]
		)
			.changesX_( false )
			.changesY_( false )
			.setSpec( \start, [0.01, 100, \exp, 0, 0.5].asSpec )
			.setSpec( \end, [0.01, 100, \exp, 0, 2].asSpec )
			.setSpec( \curve, [-16, 16, \lin, 0, 0].asSpec );
			
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
			
		WFSPathGeneratorDef(
			\lissajous,
			{ |f, path, n| 
				var startAngle, center, radius, periods;
				var close, nn = n;
				startAngle = (f.get( \startAngle ) / 360) * 2pi;
				center = f.get( \center );
				radius = f.get( \radius );
				periods = f.get( \periods );
				close = f.get( \close );
				if( close ) { nn = n-1 };
				path.positions = n.collect({ |i|
					(((i.linlin(0,nn,0,2pi * periods)) + [0,0.5pi] + startAngle)
						.sin.asPoint * radius) + center
				});
				path;
			},
			[ \periods, 1@1, \close, true, \startAngle, 0@0, \center, 0@0, \radius, 8@8 ]
		)
			.changesT_( false )
			.setSpec( \periods, PointSpec( 100, 1@1 ))
			.setSpec( \startAngle, PointSpec( 180, 0@0 ) )
			.setSpec( \center, PointSpec( 200, 0.1@0.1 ) )
			.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) );
	
	}
}