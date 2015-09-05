// jshint devel:true
jQuery.fn.center = function(parent) {
    if (parent) {
        parent = this.parent();
    } else {
        parent = window;
    }
    this.css({
        "position": "absolute",
        //"top": ((($(parent).height() - this.outerHeight()) / 2) + $(parent).scrollTop() + "px"),
        "left": ((($(parent).width() - this.outerWidth()) / 2) + $(parent).scrollLeft() + "px")
    });
    return this;
}
$(document).ready(function() {
    $('#srcAmt').keypress( function(e) {
        var chr = String.fromCharCode(e.which);
        if (("1234567890.".indexOf(chr) < 0 && e.which != 13) || ("." == chr && $("#srcAmt").val().split('.').length > 1)) {
            return false;
        }
    });
    $('select').select2({theme: "bootstrap", width: "resolve"});
    $('.select2-selection')
        .css("border-radius", "0px")
        .css("height", "34px")
        .css("margin-left", "0px")
        .css("border-left-width", "0px")
        .addClass("conversion-form");
    $('.select2-selection__rendered').css("color", "#ecf0f1");
    $('#srcCur').on("select2:open", function(e) {
        $('.select2-dropdown')
            .addClass("conversion-form")
            .css("width", "+=1")
            .css("margin-left", "-1px");
        $('.select2-results__option[aria-selected=true]')
            .css("background-color", "#1abc9c")
            .css("color", "#ecf0f1");
    });
    $('#dstCur').on("select2:open", function(e) {
        $('.select2-dropdown')
            .addClass("conversion-form")
            .css("width", "+=1")
            .css("margin-left", "-1px");
        $('.select2-results__option[aria-selected=true]')
            .css("background-color", "#1abc9c")
            .css("color", "#ecf0f1");

    });
    $(window).resize( function() {
        $('#displayArea').center();
    });
    $('#swap-button').click(function() {
        var dstCur = $('#dstCur').val();
        var srcCur = $('#srcCur').val();
        $('#dstCur').val(srcCur).trigger("change");
        $('#srcCur').val(dstCur).trigger("change");
        console.log(dstCur + " <-> " + srcCur);
    });
    $('#displayArea').center();
});
