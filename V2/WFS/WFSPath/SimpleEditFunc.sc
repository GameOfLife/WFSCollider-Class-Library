SimpleEditFunc : ObjectWithArgs {
	
	// function format:
	// { |this, objectToEdit|  ... }
	
	// defaults can be function:
	// { |this, objectToGetDefaultsFrom| ... }
	// should output an array in the same form of args:
	// [ \argName, value, \argName, value ...etc ]

	var <>func;	
	var >defaults;
	var <>specs;
	var <>action;
	var <>makeViewsFunc;
	var <>postMakeViewsFunc;
	var <>bypassFunc;
	var <>makeCopy = false;
	var <>viewHeight = 14;
	
	*new { |func, args, defaults|
		^super.new.init( name, func, args, defaults );
	}
	
	init { |inName, inFunc, inArgs, inDefaults|
		var keys, values;
		
		defaults = inDefaults ?? { inArgs.deepCopy };
		args = inArgs;
		func = inFunc;
		specs = args.clump(2).collect({ |item|
			var spec;
			spec = (spec ? item[0]).asSpec; 
			if( spec.isNil ) {
				spec = Spec.forObject( item[1] );
			};
			spec;
		});
		bypassFunc = { |f, obj| f.defaults( obj ) == f.args };
	}
	
	setSpec { |argName, spec|
		var index;
		index = this.keys.indexOf( argName );
		if( index.notNil ) {
			specs[ index ] = spec;
		} {
			"%:setSpec - argName % not found".format( this.class, argName ).warn;
		};
	}
	
	getSpec { |argName|
		var index;
		index = this.keys.indexOf( argName );
		if( index.notNil ) {
			^specs[ index ];
		} {
			"%:setSpec - argName % not found".format( this.class, argName ).warn;
			^nil;
		};
	}
	
	reset { |obj| // can use an object to get the defaults from
		this.args = defaults.value( this, obj );
	}
	
	args_ { |newArgs, constrain = false|
		newArgs.pairsDo({ |argName, value|
			this.set( argName, value, constrain );
		});
	}
	
	defaults { |obj|
		^defaults.value( this, obj );
	}
	
	set { |argName, value, constrain = false|
		var spec;
		if( constrain && { (spec = this.getSpec( argName )).notNil } ) { 
			value = spec.constrain( value );
		};
		this.setArg( argName, value );
		this.changed( argName, value );
	}
	
	get { |argName| ^this.getArg( argName ) }
	
	doesNotUnderstand { |selector ...args| 
		// bypasses errors; warning if arg not found
		if( selector.isSetter ) { 
			this.set( selector.asGetter, *args ) 
		} {
			^this.get( selector );
		};	
	}
	
	checkBypass { |obj| ^this.bypassFunc.value( this, obj ) == true }
	
	value { |obj, args|
		this.args = args;
		if( makeCopy ) { obj = obj.deepCopy };
		if( this.checkBypass( obj ) ) {
			^obj;
		} {
			^this.prValue( obj );
		};

	}
	
	prValue { |obj|
		^func.value( this, obj );
	}
	
	makeViews { |parent, bounds|
		var res;
		RoundView.useWithSkin( ( 	
				font: Font( Font.defaultSansFace, 10 ),
				labelWidth: 55
		) ++ (RoundView.skin ? ()), {
				if( makeViewsFunc.notNil ) {
					res = makeViewsFunc.value( this, parent, bounds )
				} {
					res = this.prMakeViews( parent, bounds );
				};
				postMakeViewsFunc.value( this, res );
			 }
		);
		^res;
	}
	
	viewNumLines {
		^this.specs.collect({|spec|
					if( spec.isNil ) {
						1
					} {
						spec.viewNumLines
					};
				}).sum;
	}
	
	getHeight { |margin, gap|
		viewHeight = viewHeight ? 14;
		margin = margin ?? {0@0};
		gap = gap ?? {4@4};
		^(margin.y * 2) + ( this.viewNumLines * (viewHeight + gap.y) ) - gap.y;
	}
		
	
	prMakeViews { |parent, bounds|
		var views, controller, composite;
		var margin = 0@2, gap = 0@0;
		
		if( parent.isNil ) {
			bounds = bounds ?? { 160 @ this.getHeight( margin, gap ) };
		} {
			bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
			bounds.height = this.getHeight( margin, gap );
		};
		
		controller = SimpleController( this );
		
		composite = EZCompositeView( parent, bounds, true, margin, gap ).resize_(2);
		bounds = composite.view.bounds;
		composite.onClose = {
			controller.remove
		 };
		
		views = ();
		
		views[ \composite ] = composite;
		
		this.args.pairsDo({ |key, value, i|
			var vw, spec;
			
			spec = this.specs[i/2];
			
			vw = ObjectView( composite, nil, this, key, spec, controller );
				
			vw.action = { action.value( this, key, value ); };
				
			views[ key ] = vw;
		
		});
		
		if( views.size == 0 ) {
			controller.remove;
		};
		
		^views;
	}
	
}