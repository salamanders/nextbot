if (!Math.sign) {
    Math.sign = function (x) {
        return ((x > 0) - (x < 0)) || +x;
    };
}
const reduceBy = Math.min(0.1 * (_history._durationMs / 1000.0), Math.abs(bot.motor0));
result.motor0 = bot.motor0 - Math.sign(bot.motor0) * reduceBy;