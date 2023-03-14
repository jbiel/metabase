import { Input } from "../shared/input";
import { Button } from "../shared/button";

type PasswordFormValues = {
  currentPassword?: string;
  newPassword?: string;
  newPasswordConfirmation?: string;
};

export class PasswordForm {
  private currentPasswordInput = Input.byLabel("Current password");
  private newPasswordInput = Input.byLabel("Create a password");
  private newPasswordConfirmationInput = Input.byLabel("Confirm your password");
  private submitButton = Button.byLabel("Save");

  verifyValues = ({
    currentPassword,
    newPassword,
    newPasswordConfirmation,
  }: PasswordFormValues) => {
    if (currentPassword != null) {
      this.currentPasswordInput.verifyValue(currentPassword);
    }

    if (newPassword != null) {
      this.newPasswordInput.verifyValue(newPassword);
    }

    if (newPasswordConfirmation != null) {
      this.newPasswordConfirmationInput.verifyValue(newPasswordConfirmation);
    }

    return this;
  };

  fill = ({
    currentPassword,
    newPassword,
    newPasswordConfirmation,
  }: PasswordFormValues) => {
    if (currentPassword != null) {
      this.currentPasswordInput.setValue(currentPassword);
    }

    if (newPassword != null) {
      this.newPasswordInput.setValue(newPassword);
    }

    if (newPasswordConfirmation != null) {
      this.newPasswordConfirmationInput.setValue(newPasswordConfirmation);
    }

    return this;
  };

  submit = () => {
    this.submitButton.click();
    return this;
  };

  verifySubmitDisabled = () => {
    this.submitButton.verifyDisabled();
    return this;
  };
}