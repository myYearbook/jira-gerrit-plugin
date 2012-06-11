/*
AJS.toInit(function() {
    var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");

    function getFormData() {
        return { hostname: AJS.$("#gerritHostname").attr("value").trim(),
                 username: AJS.$("#gerritUsername").attr("value").trim(),
                 port: parseInt( AJS.$("#gerritPort").attr("value"), 10 ) };
    }

    function stringifyData(data) {
        return '{"hostname": "' + data.hostname + '", '
               + '"port": ' + data.port + ', '
               + '"username": "' + data.username + '"}';
    }
    
    function populateForm() {
        AJS.$.ajax({
            url: baseUrl + "/rest/gerrit-admin/1.0/",
            dataType: "json",
            success: function(config) {
                AJS.$("#gerritHostname").attr("value", config.hostname);
                AJS.$("#gerritPort").attr("value", config.port);
                AJS.$("#gerritUsername").attr("value", config.username);
            }
        });
    }
    
    function updateConfig() {
        var data = getFormData();
        data = stringifyData(data);

        AJS.$.ajax({
            url: baseUrl + "/rest/gerrit-admin/1.0/",
            type: "PUT",
            contentType: "application/json",
            data: data,
            processData: false
        });
    }
    
    populateForm();
    
    AJS.$("#gerritTest").bind('click', function(e) {
        var data = getFormData();

        if (isNaN(data.port) || data.port < 0) {
            alert("Invalid port!  Must be numeric");
            return false;
        }

        data = stringifyData(data);
        AJS.$.ajax({
            url: baseUrl + "/rest/gerrit-admin/1.0/",
            type: "POST",
            contentType: "application/json",
            data: data,
            success: function(data) {
            
            }
        });
    });

    AJS.$("#admin").submit(function(e) {
        e.preventDefault();
        updateConfig();
    });
});
*/