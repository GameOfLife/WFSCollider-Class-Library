SyncCenterGui {
	classvar <window, <controllers;
	
	*new{
		var font = Font( Font.defaultSansFace, 11 ), masterText, masterUV, readyUV;		
		
		controllers = List.new;
		
		if( window.notNil ){
			if( window.isClosed.not) {
				controllers.do(_.remove);
				window.close
			};
			window = nil;
		};
		window = Window("Sync Center",Rect(300,300,400,400)).front;
		window.addFlowLayout;
		
		SmoothButton( window, 100@30  )
 			.states_( [[ "Sync", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.action_({ 
				SyncCenter.remoteSync			
			});
			
		StaticText(window,100@20).string_("SyncStatus:");
		readyUV = UserView(window,20@20).background_(Color.red);
		controllers.add(Updater(SyncCenter.ready, { |ready|
			{
				readyUV.background_( if( ready.value ) {Color.green} {Color.red} )
			}.defer
		}));
		
		
		window.view.decorator.nextLine;
		
		masterText = StaticText(window,100@20).string_("Master:");
		masterUV = UserView(window,20@20).background_(Color.red);
		
		controllers.add(Updater(SyncCenter.masterCount, { |mCount|
			{
				masterText.string_("Master: "++mCount.value);
				masterUV.background_(Color.green)
			}.defer
		}));
		window.view.decorator.nextLine;
		SyncCenter.servers.do{ |server,i|
			var text, uv;
			text = StaticText(window,100@20).string_(server.name++":" );
			uv = UserView(window,20@20).background_(Color.red);
			controllers.add(Updater(SyncCenter.remoteCounts[i], { |count|
				{
					text.string_(server.name++": "+count.value);
					uv.background_( if( count.value != -1 ) {Color.green} {Color.red} );
				}.defer
			}));
			
			window.view.decorator.nextLine;
		};
		
		window.onClose_({ this.remove });
			
	}
	
	remove{
		controllers.do(_.remove);
	}
}

SyncCenterStatusWidget{
	var <controller, <view;
	
	*new{ |parent, size = 20|
		^super.new.init(parent,size)
	}
	
	init{ |parent,size| 
		view = UserView(parent,size@size).background_(Color.red);
		controller = Updater(SyncCenter.ready, { |ready|
			{
				view.background_( if( ready.value ) {Color.green(0.7) } {Color.red(0.7) } )
			}.defer
		})
	}
	
	remove{
		controller.remove
	}
}

SyncCenterServerWidget{
	var <controller, <view;
	
	*new{ |parent, bounds, server|
		if(SyncCenter.servers.includes(server) ) {
			^super.new.init(parent,bounds,server)
		}
	}
	
	init{ |parent,bounds,server| 
		view = RoundNumberBox(parent,bounds).background_(Color.red);
		controller = Updater(SyncCenter.remoteCounts[SyncCenter.servers.indexOf(server)], { |count|
			{
				view.value_(count.value);
				view.background_( if( count.value != -1 ) { Color.green(0.7) } { Color.red(0.7) } );
			}.defer
		})
	}
	
	remove{
		controller.remove
	}
}

	
		