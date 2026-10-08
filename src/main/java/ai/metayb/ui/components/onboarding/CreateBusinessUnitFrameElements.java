package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Locators for the frame of the Create Business Unit dialog (its modal attributes, title chips and surroundings), from
 * the live DOM. Sections, the name field, Cancel and the header close buttons stay with
 * {@link CreateBusinessUnitDialogElements}.
 *
 * The dialog is MUI's: {@code div[role=dialog]} (the paper) inside a full-viewport {@code div[role=presentation]}
 * container, which is what receives a click outside the dialog (see {@code TenantCronsElements}). The "New" and
 * "Unsaved changes" chips are labels inside the title; the unsaved-changes chip exists only while some value differs
 * from its default. A confirmation, if the portal ever asked one, would be an alert dialog, a SweetAlert popup or
 * another dialog, so the confirmation locator is any of those other than this dialog itself.
 */
@Getter
public class CreateBusinessUnitFrameElements {

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String CONTAINER_XPATH = DIALOG + "/parent::div[@role='presentation']";
    private static final String TITLE_XPATH = DIALOG + "//h6[normalize-space()='Create Business Unit']";
    private static final String NEW_CHIP_XPATH = DIALOG + "//h2//span[normalize-space()='New']";
    private static final String UNSAVED_CHIP_XPATH = DIALOG + "//h2//span[normalize-space()='Unsaved changes']";
    private static final String CONFIRMATION_XPATH = "//*[@role='alertdialog'] | //div[contains(@class,'swal2-popup')]"
            + " | //div[@role='dialog'][not(.//h2[starts-with(normalize-space(),'Create Business Unit')])]";

    private final By dialogLocator = By.xpath(DIALOG);
    private final By unsavedChipLocator = By.xpath(UNSAVED_CHIP_XPATH);
    private final By confirmationLocator = By.xpath(CONFIRMATION_XPATH);

    @FindBy(xpath = DIALOG)
    private WebElement dialog;

    @FindBy(xpath = CONTAINER_XPATH)
    private WebElement container;

    @FindBy(xpath = TITLE_XPATH)
    private WebElement title;

    @FindBy(xpath = NEW_CHIP_XPATH)
    private WebElement newChip;
}
