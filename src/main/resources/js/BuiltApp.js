define("Config",[],function(){return{urlBase:"/rest/privaterepos/1.0"}});define("Group",["backbone","Config"],function(Backbone,Config){return Backbone.Model.extend({urlRoot:Config.urlBase+"/groups/group",idAttribute:"name"})});define("GroupBatch",["backbone","Config"],function(Backbone,Config){return Backbone.Model.extend({defaults:{names:[]},url:Config.urlBase+"/groups/list"})});define("Groups",["backbone","Group","Config"],function(Backbone,Group,Config){return Backbone.Collection.extend({url:Config.urlBase+"/groups/list",model:Group})});define("User",["backbone","Config"],function(Backbone,Config){return Backbone.Model.extend({urlRoot:Config.urlBase+"/users/user",idAttribute:"name"})});define("UserBatch",["backbone","Config"],function(Backbone,Config){return Backbone.Model.extend({defaults:{names:[]},url:Config.urlBase+"/users/list"})});define("Users",["backbone","User","Config"],function(Backbone,User,Config){return Backbone.Collection.extend({url:Config.urlBase+"/users/list",model:User})});define("GroupRow",["backbone"],function(Backbone){return Backbone.View.extend({tagName:"tr",template:PrivateRepos.groupRow,events:{"click .delete-button":"onDelete"},onDelete:function(e){e.preventDefault();this.model.destroy();this.remove()},render:function(){this.el.innerHTML=this.template({group:this.model.toJSON()});return this}})});define("GroupsTable",["Table","GroupRow","underscore","GroupBatch","Config"],function(Table,GroupRow,_,GroupBatch,Config){return Table.extend({initialize:function(){Table.prototype.initialize.apply(this,arguments);_.bindAll(this,"onAllowSuccess","handleAllow")},template:PrivateRepos.table,itemView:GroupRow,searchUrl:function(term){return Config.urlBase+"/groups/find/"+term},handleAllow:function(values){var groupBatch=new GroupBatch({names:values});groupBatch.save({},{success:this.onAllowSuccess})},onAllowSuccess:function(){this.collection.fetch();this.clearSearchInput()}})});define("Table",["backbone","underscore"],function(Backbone,_){return Backbone.View.extend({tagName:"table",className:"private-repos-permissions-table aui",events:{"click .allow-button":"onAllow"},initialize:function(options){_.bindAll(this,"addChildView");this.collection=options.collection;this.collection.on("add",this.addChildView);this.collection.on("reset",this.addChildViews)},addChildViews:function(){this.collection.each(function(model){this.addChildView(model)},this)},addChildView:function(model){var itemViewClass=this.itemView;this.$("tbody").append(new itemViewClass({model:model}).render().el)},render:function(){this.$el.html(this.template());var searchInput=this.$(".search-input");searchInput.auiSelect2({hasAvatar:true,multiple:true,minimumInputLength:1,ajax:{url:this.searchUrl,dataType:"json",results:function(data,page){results=_.map(data,function(object){return{id:object.name,text:object.name}});return{results:results}},formatResult:function(object){return object.name}}});return this},onAllow:function(e){e.preventDefault();var val=this.$(".search-input").select2("val");this.handleAllow(val)},clearSearchInput:function(){this.$(".search-input").select2("val",null)}})});define("UserRow",["backbone"],function(Backbone){return Backbone.View.extend({tagName:"tr",template:PrivateRepos.userRow,events:{"click .delete-button":"onDelete"},onDelete:function(e){e.preventDefault();this.model.destroy();this.remove()},render:function(){this.el.innerHTML=this.template({user:this.model.toJSON()});return this}})});define("UsersTable",["Table","UserRow","UserBatch","Config"],function(Table,UserRow,UserBatch,Config){return Table.extend({template:PrivateRepos.table,itemView:UserRow,initialize:function(){Table.prototype.initialize.apply(this,arguments);_.bindAll(this,"onAllowSuccess","handleAllow")},searchUrl:function(term){return Config.urlBase+"/users/find/"+term},handleAllow:function(values){var userBatch=new UserBatch({names:values});userBatch.save({},{success:this.onAllowSuccess})},onAllowSuccess:function(){this.collection.fetch();this.clearSearchInput()}})});define("PrivateRepos",["jquery","UsersTable","Users","GroupsTable","Groups"],function($,UsersTable,Users,GroupsTable,Groups){var constr=function(opts){this.initialize(opts)};_.extend(constr.prototype,{initialize:function(opts){},start:function(){this.startUsersTable();this.startGroupsTable()},startUsersTable:function(){var users=new Users;var usersTable=new UsersTable({collection:users});$("#users-table").html(usersTable.render().el);users.fetch()},startGroupsTable:function(){var groups=new Groups;var groupsTable=new GroupsTable({collection:groups});$("#groups-table").html(groupsTable.render().el);groups.fetch()}});return constr});AJS.$(document).ready(function($){var go=function(){require(["PrivateRepos"],function(PrivateRepos){console.log("Starting Private Repos");var privateRepos=new PrivateRepos;privateRepos.start()})};setTimeout(go,10)}(AJS.$));