WFSOptionsGUI {
	
	classvar <>current;
	classvar <>columnWidth = 220;
	
	var <object;
	var <view, <firstColumn, <secondColumn, <optionsView;
	var <masterComp, <masterHeader, <masterView;
	var <serverComp, <serverHeader, <serverViews;
	
	*new { |parent, bounds, object|
		^super.new.init( parent, bounds, object )
	}
	
	*newOrCurrent {
		if( current.notNil && { current.composite.isClosed.not } ) {
			current.composite.getParents.last.findWindow.front;
			^current;
		} {
			^this.new;
		};
	}
	
	init { |parent, bounds, inObject|
		
		var ctrl;
		
		
		
		if( parent.isNil ) { 
			parent = this.class.asString;
			bounds = bounds ?? { Rect( 
					190 rrand: 220, 
					70 rrand: 100,
					2 * (columnWidth + 12), 
					412
 				 ) 
			}; 
		} {
			bounds = parent.asView.bounds;
		};
		
		object = inObject ? WFSOptions.current ?? { WFSOptions.fromPreset( \default ); };
		
		view = EZCompositeView( parent, bounds, gap: 2@2, margin: 2@2 );
		view.resize_(5);
		bounds = view.asView.bounds;
		view.addFlowLayout(2@2, 6@6);
		
		RoundView.pushSkin( UChainGUI.skin );
		
		firstColumn = CompositeView( view, columnWidth @ bounds.height )
			.background_( Color.gray(1).alpha_(0.125) )
			.resize_(4);
			
		firstColumn.addFlowLayout(0@0, 2@2);
		
		secondColumn = CompositeView( view, columnWidth @ bounds.height )
			.background_( Color.gray(1).alpha_(0.125) )
			.resize_(5);
			
		secondColumn.addFlowLayout(0@0, 2@2);
		
		StaticText( firstColumn, columnWidth @ 14 )
			.applySkin( RoundView.skin )
			.string_( " options" )
			.background_( Color.gray(0.8).alpha_(0.5) );
		
		optionsView = WFSOptionsObjectGUI( firstColumn, columnWidth @ bounds.height, object );
		
		firstColumn.decorator.shift( 0, 14 );
			
		masterHeader = CompositeView( firstColumn, columnWidth @ 14 )
			.background_( Color.gray(0.8).alpha_(0.5) );
			
		masterHeader.addFlowLayout( 0@0, 2@2 );
			
		StaticText( masterHeader, columnWidth - (2 + 14) @ 14 )
			.applySkin( RoundView.skin )
			.string_( " master server" );

		SmoothButton( masterHeader, 14 @ 14 )
			.label_([ "", 'x' ])
			.radius_(2)
			.value_( object.masterOptions.notNil.binaryValue )
			.action_({ |bt|
				if( bt.value == 1 ) {
					object.masterOptions = object.masterOptions ?? { WFSMasterOptions().useForWFS_(true) };
				} {
					object.masterOptions = nil;
				};
				this.makeMasterGUI;
			});		
		
		masterComp = CompositeView( firstColumn, columnWidth @ WFSMasterOptionsGUI.getHeight );
		
		this.makeMasterGUI;
		
		serverHeader = CompositeView( secondColumn, columnWidth @ 14 )
			.background_( Color.gray(0.8).alpha_(0.5) )
			.resize_( 2 );
			
		serverHeader.addFlowLayout( 0@0, 2@2 );
			
		StaticText( serverHeader, columnWidth - ((2 + 14) * 2) @ 14 )
			.applySkin( RoundView.skin )
			.string_( " servers" );

		SmoothButton( serverHeader, 14 @ 14 )
			.label_('-')
			.action_({
				object.serverOptions = object.serverOptions[..object.serverOptions.size-2];
				this.makeServersGUI;
			})
			.resize_( 3 );
		
		SmoothButton( serverHeader, 14 @ 14 )
			.label_('+')
			.action_({
				var last;
				last = object.serverOptions.last;
				if( last.notNil ) {
					last = last.copy;
					if( last.ip == "127.0.0.1" ) {
						last.ip = NetAddr.myIP;
					};
					if( last.ip != "127.0.0.1" ) {
						last.ip = PathName(last.ip).nextName;					};
					last.name = PathName(last.name).nextName;
				} {
					last = WFSServerOptions();
					if( object.masterOptions.notNil ) {
						last.ip = PathName( NetAddr.myIP ).nextName;
					};
				};
				object.serverOptions = object.serverOptions ++ [ last ];
				this.makeServersGUI;
			})
			.resize_( 3 );
			
		serverComp = CompositeView( secondColumn, columnWidth @ (secondColumn.bounds.height - 16) )
			.resize_(5);
		
		serverComp.addFlowLayout( 0@0, 2@2 );	
					
		this.makeServersGUI;
		
		RoundView.popSkin;

		current = this;
		this.class.changed( \current );
		
		view.findWindow.toFrontAction = { 
			current = this;
			this.class.changed( \current );
		};
		
		view.onClose = view.onClose.addFunc( { 
			//ctrl.remove;
			if( current == this ) {
				current = nil;
				this.class.changed( \current );
			};
		} );
	
	}
	
	makeMasterGUI {
		
		masterView !? _.remove;
		masterComp.refresh;
				
		if( object.masterOptions.notNil ) {
			masterView = WFSMasterOptionsGUI( masterComp, columnWidth @ 14, object.masterOptions )
				.background_( Color.gray(0.3).alpha_(0.25) ); 
		} {
			masterView = nil;
		};
	}
	
	makeServersGUI {
		serverViews.do(_.remove);
		serverComp.refresh;
		serverComp.decorator.reset;
		
		serverViews = object.serverOptions.collect({ |item|
			WFSServerOptionsGUI( serverComp, serverComp.bounds.width @ 14, item )
				.background_( Color.gray(0.3).alpha_(0.25) );
		});
	}
	
	object_ { |obj| 
		optionsView.object = obj;
		
	 }
	
	doesNotUnderstand { |selector ...args|
		var res;
		res = optionsView.perform( selector, *args );
		if( res != optionsView ) { ^res; }
	}
	
}

+ WFSOptions {
	gui { |parent, bounds| ^WFSOptionsGUI( parent, bounds, this ) }
}