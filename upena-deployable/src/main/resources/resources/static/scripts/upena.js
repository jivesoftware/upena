window.$ = window.jQuery;

window.upena = {};

upena.hs = {
    installed: null,
    queuedUninstall: null,
    init: function () {
        $('input[type=text].upena-hs-field').each(function (i, input) {
            var $inputName = $(input);
            var $inputKey = $(input).next('input[type=hidden]');
            var endpoint = $inputName.data('upenaLookup');
            $inputName.focus(function () {
                upena.hs.install($inputKey, $inputName, function (key, name) {
                    $inputKey.val(key);
                    $inputName.val(name);
                });
                upena.hs.lookup(endpoint, $inputName.val());
            });
            $inputName.on('input', function () {
                $inputKey.val('');
                upena.hs.lookup(endpoint, $inputName.val());
            });
            $inputName.blur(function () {
                upena.hs.queuedUninstall = setTimeout(function () {
                    upena.hs.uninstall($inputName);
                }, 200);
            });
        });
    },
    install: function ($inputKey, $inputName, callback) {
        upena.hs.cancelUninstall();
        upena.hs.uninstall();

        var $selector = upena.hs.makeSelector();
        $('body').append($selector);

        upena.hs.installed = {
            selector: $selector,
            inputKey: $inputKey,
            inputName: $inputName,
            callback: callback,
            ready: false
        };

        $inputName.removeClass('upena-hs-field-broken');

        var offset = $inputName.offset();
        var height = $inputName.height();
        $selector.show();
        $selector.offset({
            left: offset.left,
            top: offset.top + height + 10
        });
    },
    uninstall: function ($inputName) {
        if (!upena.hs.installed || $inputName && upena.hs.installed.inputName != $inputName) {
            return;
        }

        $inputName = $inputName || upena.hs.installed.inputName;
        var $inputKey = upena.hs.installed.inputKey;
        var name = $inputName.val();
        var found = false;

        var $selector = upena.hs.installed.selector;
        $selector.find('a').each(function (i) {
            var $a = $(this);
            if ($a.data('upenaName') == name) {
                var key = $a.data('upenaKey');
                $inputKey.val(key);
                found = true;
                return false;
            }
        });

        if (!found) {
            $inputName.addClass('upena-hs-field-broken');
            upena.hs.installed.inputKey.val('');
        }

        $selector.remove();
        upena.hs.installed = null;
    },
    makeSelector: function () {
        var $selector = $('<div>').addClass("upena-hs-selector");
        $selector.focus(function () {
            if (selector == upena.hs.installed.selector) {
                upena.hs.cancelUninstall();
            }
        });
        $selector.blur(function () {
            upena.hs.uninstall();
        });
        return $selector;
    },
    cancelUninstall: function () {
        if (upena.hs.queuedUninstall) {
            clearTimeout(upena.hs.queuedUninstall);
        }
    },
    picked: function (key, name) {
        if (upena.hs.installed) {
            upena.hs.installed.callback(key, name);
            upena.hs.uninstall();
        }
    },
    lookup: function (endpoint, contains) {
        var $selector = upena.hs.installed.selector;
        $.ajax(endpoint, {data: {'contains': contains}})
                .done(function (data) {
                    if (!upena.hs.installed || upena.hs.installed.selector != $selector) {
                        // selector changed during the query
                        return;
                    }
                    if (data.length) {
                        $selector.empty();
                        for (var i = 0; i < data.length; i++) {
                            $selector.append(
                                    "<a href='#'" +
                                    " class='upena-hs-choice'" +
                                    " data-upena-key='" + data[i].key + "'" +
                                    " data-upena-name='" + data[i].name + "'>" + data[i].name + "</a><br/>");
                        }
                        upena.hs.link($selector);
                        upena.hs.installed.ready = true;
                    } else {
                        $selector.html("<em>No matches</em>");
                    }
                });
    },
    link: function ($selector) {
        $selector.find('a').each(function (i) {
            var $a = $(this);
            var key = $a.data('upenaKey');
            var name = $a.data('upenaName');
            $a.click(function () {
                upena.hs.picked(key, name);
                return false;
            });
        });
    }
};

upena.instances = {
    init: function () {
    }
};

upena.clusterReleaseGroups = {
    add: function (clusterId) {
        var serviceId = $('#serviceId-' + clusterId).val();
        var releaseGroupId = $('#releaseId-' + clusterId).val();

        $.ajax("/ui/clusters/add", {
            data: JSON.stringify({'clusterId': clusterId, 'serviceId': serviceId, 'releaseGroupId': releaseGroupId}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    remove: function (clusterId, serviceId, releaseGroupId) {
        $.ajax("/ui/clusters/remove", {
            data: JSON.stringify({'clusterId': clusterId, 'serviceId': serviceId, 'releaseGroupId': releaseGroupId}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    }
};

upena.instancePorts = {
    addPort: function (instanceId) {
        var portName = $('#portName-' + instanceId).val();
        var port = $('#port-' + instanceId).val();
        var propertyName = null;
        var propertyValue = null;

        console.log(portName + " " + port + " " + instanceId);

        $.ajax("/ui/instances/ports/add", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    removePort: function (instanceId, portName) {
        var port = -1;
        var propertyName = null;
        var propertyValue = null;

        $.ajax("/ui/instances/ports/remove", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    addPortProperty: function (instanceId, portName) {
        var port = -1;
        var propertyName = $('#portPropertyName-' + instanceId + '-' + portName).val();
        var propertyValue = $('#portPropertyValue-' + instanceId + '-' + portName).val();

        $.ajax("/ui/instances/ports/add", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    removePortProperty: function (instanceId, portName, propertyName) {
        var port = -1;
        var propertyValue = null;

        $.ajax("/ui/instances/ports/remove", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    }
};


upena.cfg = {
    pending: {},
    save: function () {
        var updates = [];
        $.each(upena.cfg.pending, function (prop) {
            $.each(upena.cfg.pending[prop], function (instanceKey) {
                var value = upena.cfg.pending[prop][instanceKey] || "(default)";
                updates.push(instanceKey, ': ', prop, ' -> ', value, '\n');
            });
        });
        if (confirm(updates.join(''))) {
            $.ajax("/ui/config/modify", {
                data: JSON.stringify({'updates': upena.cfg.pending}),
                method: "post",
                contentType: "application/json",
                success: function () {
                    upena.cfg.pending = {};
                    upena.cfg.refreshPending();
                },
                error: function () {
                    alert('Save failed!');
                }
            });
        }
    },
    init: function () {
        $('#pending-save').click(upena.cfg.save);

        $('button.upena-cfg-link').each(function (i) {
            var $button = $(this);
            var link = $button.data('upenaCfgLink');
            var prop = $button.data('upenaCfgProp');

            $button.click(function () {
                var value = $('input[type=text][data-upena-cfg-link="' + link + '"]').val();
                $('input[type=text][data-upena-cfg-prop="' + prop + '"].upena-cfg-field-a').each(function () {
                    $(this).val(value);
                    upena.cfg.checkInput($(this));
                });
            });
        });

        $('button.upena-cfg-copy').each(function (i) {
            var $button = $(this);
            var copy = $button.data('upenaCfgCopy');
            var prop = $button.data('upenaCfgProp');

            $button.click(function () {
                var value = $('input[type=text][data-upena-cfg-copy="' + copy + '"]').val();
                $('input[type=text][data-upena-cfg-prop="' + prop + '"].upena-cfg-field-a').each(function () {
                    $(this).val(value);
                    upena.cfg.checkInput($(this));
                });
            });
        });

        $('input[type=text].upena-cfg-field-a').on('input', function () {
            upena.cfg.checkInput($(this));
        });
    },
    checkInput: function ($input) {
        var instanceKey = $input.data('upenaCfgInstanceKey');
        var prop = $input.data('upenaCfgProp');
        var override = $input.data('upenaCfgOverride');
        var value = $input.val();
        var modified = (override != value);
        if (!upena.cfg.pending[prop]) {
            upena.cfg.pending[prop] = {};
        }
        if (modified) {
            upena.cfg.pending[prop][instanceKey] = value;
        } else {
            delete upena.cfg.pending[prop][instanceKey];
        }
        upena.cfg.refreshPending();
    },
    refreshPending: function () {
        var $fixed = $('#upena-cfg-pending');
        var $pending = $('#pending-count');
        var changes = 0;
        $.each(upena.cfg.pending, function (prop) {
            $.each(upena.cfg.pending[prop], function (instanceKey) {
                changes++;
            });
        });
        $pending.text(changes);
        if (changes > 0) {
            $fixed.show();
        } else {
            $fixed.hide();
        }
    }
};

upena.topology = {
    layouter: null,
    renderer: null,
    redraw: null,
    height: null,
    width: null,
    init: function () {

        upena.topology.height = "600";
        upena.topology.width = $(document).width() - 100;
        var nodes = $('#upena-topology').data('nodes');
        var edges = $('#upena-topology').data('edges');


        /* http://www.graphdracula.net/ */
        var g = new Graph();

        $(nodes).each(function (key, node) {
            var render = function (r, n) {


                /* the Raphael set is obligatory, containing all you want to display */
                var text = r.text(n.point[0], n.point[1], n.label).attr({"font-size": node.fontSize + "px", opacity: 1.0, fill: "#000"});
                var bb = text.getBBox(true);
                var w = bb.width + 12;
                var h = bb.height + 12;
                var rect = r.rect(n.point[0] - (w / 2), n.point[1] - (h / 2), w, h).attr({
                    fill: "270-#" + node.maxbgcolor + "-#" + node.minbgcolor,
                    r: "6px",
                    "stroke-width": n.distance == 0 ? "3px" : "1px"
                });

                var set = r.set().push(rect).push(text);
                text.toFront();
                return set;
            };
            var clicked = function () {
                $("#topology-health").html(node.focusHtml);
                return;
            }
            g.addNode(node.id, {label: node.label, render: render, clicked: clicked});
        });


        $(edges).each(function (key, edge) {
            g.addEdge(edge.from, edge.to, {label: edge.label, directed: true, stroke: "#" + edge.color, fill: "#" + edge.color});
        });


        /* layout the graph using the Spring layout implementation */
        upena.topology.layouter = new Graph.Layout.Spring(g);
        upena.topology.layouter.layout();

        /* draw the graph using the RaphaelJS draw implementation */
        upena.topology.renderer = new Graph.Renderer.Raphael('upena-topology', g, upena.topology.width, upena.topology.height);
        upena.topology.renderer.draw();

    },
    redraw : function () {
        dracula_graph_seed = 1;
        upena.topology.layouter.layout();
        upena.topology.renderer.draw();
    }
}

$(document).ready(function () {
    if ($('.upena-hs-field').length) {
        upena.hs.init();
    }
    if ($('#upena-instances').length) {
        upena.instances.init();
    }
    if ($('#upena-cfg').length) {
        upena.cfg.init();
    }

    if ($('#upena-topology').length) {
        upena.topology.init();
    }

    $(function () {
        var hack = {};
        $('[rel="popover"]').popover({
            container: 'body',
            html: true,
            content: function () {
                console.log($(this).attr('id'));
                var h = $($(this).data('popover-content')).removeClass('hide');
                hack[$(this).attr('id')] = h;
                return h;
            }
        }).click(function (e) {
            e.preventDefault();
        }).on('hidden.bs.popover', function () {
            var h = hack[$(this).attr('id')];
            if (h) {
                h.detach();
                h.addClass('hide');
                $('body').append(h);
            }
        });
    });

    $(function () {
        var hack = {};
        $('[rel="popover-health"]').popover({
            container: 'body',
            html: true,
            content: function () {
                console.log($(this).attr('id'));
                var h = $($(this).data('popover-content')).removeClass('hide');
                hack[$(this).attr('id')] = h;
                return h;
            }
        }).click(function (e) {
            e.preventDefault();
        }).on('show.bs.popover', function () {
            var instanceKey = $(this).data('popoverInstanceKey');
            if (instanceKey) {
                var h = hack[$(this).attr('id')];
                $.ajax("/ui/health/uis", {
                    method: "get",
                    data: {"instanceKey": instanceKey},
                    success: function (data) {
                        $(h).find(".uis").html(data);
                    },
                    error: function () {
                        $(h).find(".uis").html("Failed to load UIs");
                    }
                });
            }

        }).on('hidden.bs.popover', function () {
            var h = hack[$(this).attr('id')];
            if (h) {
                h.detach();
                h.addClass('hide');
                $('body').append(h);
            }
        });
    });

});