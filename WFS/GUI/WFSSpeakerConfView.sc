WFSSpeakerConfView : WFSBasicEditView {
	
	defaultObject	{ ^nil } // nil means default
	
	conf { ^object ? WFSSpeakerConf.default; }
	
	drawContents { |scale = 1|
		var conf, lines;
		
		scale = scale.asArray;
		
		conf = this.conf;
		
		if( conf.notNil ) {
			Pen.use({
				// show selection
				Pen.scale(1,-1);
				lines = conf.asLines;
				
				if( selected.size > 0 ) {
					
					Pen.width = 0.164 * 2;
					Pen.color = Color.yellow;
					
					lines.do({ |line, i|
						if( selected.includes( i ) ) {
							Pen.line( *line );
						};
					});
					Pen.stroke;
					
				};
			});
			
			Pen.use({
				
				Pen.width = 0.164;
				Pen.color = Color.red(0.5, 0.5);
					
				//// draw configuration
				conf.draw;
				
			});
		};
		
	}
	
	select { |index| // can only select one
		if( index.notNil && { index < this.conf.size }) {
			selected = [index];
		} {
			selected = [];
		};
		this.refresh;
		this.changed( \select );
	}
	
	zoomToFit {
		view.viewRect_( 
			(this.conf).asRect.scale(1@(-1)).insetBy(-1,-1) 
		);  
	}
	
	getNearestIndex { |point, scaler| // returns nil if outside radius
		var radius;
		var conf;
		conf = this.conf;
		if( conf.notNil ) {
			radius = scaler.asArray.mean * 5;
			^conf.arrayConfs.detectIndex({ |arr, i|
				var pt;
				pt = point.rotate( arr.angle.neg );
				pt.x.inclusivelyBetween( arr.dist - radius, arr.dist + radius ) &&
				{
					pt.y.inclusivelyBetween( 
						arr.rotatedFirstPoint.y - radius,
						arr.rotatedLastPoint.y + radius
					);
				};
			});
		} {
			^nil;
		};
	}
	
	
	getIndicesInRect { |rect|
		/*
		TODO
		var conf, index, corners;
		conf = this.conf;
		if( conf.notNil ) {
			corners = [ rect.leftTop, rect.rightTop, rect.leftBottom, rect.rightBottom ];
			index = conf.arrayConfs.detectIndex({ |arr, i|
				var pts;
				pts = corners.collect(_.rotate( arr.angle.neg ));
				arr.dist.inclusivelyBetween( 
					pts.collect(_.x).minItem, 
					pts.collect(_.x).maxItem
				) &&
				{
					
					pt.y.inclusivelyBetween( 
						arr.rotatedFirstPoint.y - radius,
						arr.rotatedLastPoint.y + radius
					);
				};
			});
		};
		^index ? [];
		*/
		^[]				
	}
	
	mouseEditSelected {
	}
	
	
}