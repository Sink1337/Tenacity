package dev.meguru.ui.sidegui.panels.infopanel;

import dev.meguru.ui.Screen;
import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.font.FontUtil;
import dev.meguru.utils.misc.HoveringUtil;
import dev.meguru.utils.render.ColorUtil;
import dev.meguru.utils.render.RenderUtil;
import dev.meguru.utils.render.RoundedUtil;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.List;

@Getter
@Setter
public class InfoButton implements Screen {
    private final String question, answer;

    private float x, y, width, height, alpha, count = 1;

    private final Animation openAnimation = new DecelerateAnimation(250, 1).setDirection(Direction.BACKWARDS);
    private final Animation hoverAnimation = new DecelerateAnimation(250, 1).setDirection(Direction.BACKWARDS);

    public InfoButton(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    @Override
    public void initGui() {

    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Color textColor = ColorUtil.applyOpacity(Color.WHITE, alpha);
        boolean hovering = HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY);
        hoverAnimation.setDirection(hovering ? Direction.FORWARDS : Direction.BACKWARDS);
        hoverAnimation.setDuration(hovering ? 200 : 400);


        float additionalCount = 0;
        if (!openAnimation.isDone() || openAnimation.finished(Direction.FORWARDS)) {
            float heightIncrement = 3;
            float openAnim = openAnimation.getOutput().floatValue();
            List<String> lines = tenacityFont16.getWrappedLines(answer, x + 5, (width - 5), heightIncrement);
            int spacing = 3;
            float totalAnswerHeight = (lines.size() * (tenacityFont16.getHeight() + heightIncrement)) + 4;
            float additionalHeight = height + totalAnswerHeight + (spacing * 2);

            RenderUtil.scissorStart(x - 1, y + 5, width + 2, additionalHeight - 5);

            float answerY = (y + height + spacing) - (((spacing + totalAnswerHeight) * (1 - openAnim)));
            RoundedUtil.drawRound(x, answerY, width, totalAnswerHeight, 5, ColorUtil.tripleColor(55, alpha));


            for (String line : lines) {
                tenacityFont16.drawString(line, x + 3, answerY + 3.5f, textColor);
                answerY += tenacityFont16.getHeight() + heightIncrement;
            }
            RenderUtil.scissorEnd();


            additionalCount = ((totalAnswerHeight + spacing) * openAnim) / height;
        }


        int additionalColor = (int) (5 * hoverAnimation.getOutput().floatValue());
        RoundedUtil.drawRound(x, y, width, height, 5, ColorUtil.tripleColor(37 + additionalColor, alpha));
        //Question Text
        tenacityBoldFont18.drawString(question, x + 5, y + tenacityBoldFont18.getMiddleOfBox(height), textColor);


        float iconX = x + width - (iconFont20.getStringWidth(FontUtil.DROPDOWN_ARROW) + 5);
        float iconY = y + iconFont20.getMiddleOfBox(height) + 1;

        RenderUtil.rotateStart(iconX, iconY, iconFont20.getStringWidth(FontUtil.DROPDOWN_ARROW), iconFont20.getHeight(), 180 * openAnimation.getOutput().floatValue());
        iconFont20.drawString(FontUtil.DROPDOWN_ARROW, iconX, iconY, textColor);
        RenderUtil.rotateEnd();


        count = 1 + additionalCount;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (HoveringUtil.isHovering(x, y, width, height, mouseX, mouseY) && button == 1) {
            openAnimation.changeDirection();
        }

    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }
}
