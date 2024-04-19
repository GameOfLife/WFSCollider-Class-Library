WFSPathGeneratorDef(
	\round,
	{ |f, path, n|
		var precision, amount, mode;
		var current, mean, extent, size;
		current = path.positions.deepCopy;
		precision = f.get( \precision );
		mode = f.get( \mode );
		switch( mode,
			\xy, {
				current = current.collect(_.round( precision ) );
			},
			\angle, {
				current = current.collect({ |pt|
					pt.angle = (pt.angle / pi * 180).round( precision ) / 180 * pi;
				});
			},
			\distance, {
				current = current.collect({ |pt|
					pt.rho = pt.rho.round( precision );
				});
			},
		);
		path.positions = current;
	},
	[ \precision, 1.0, \mode, \xy ]
)
.changesT_( false )
.setSpec( \precision, ControlSpec(0,200, 10.calcCurve(0,200), 0, 1 ) )
.setSpec( \mode, ListSpec([\xy, \angle, \distance]) );