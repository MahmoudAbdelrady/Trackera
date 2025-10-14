import { Link, Lock } from "lucide-react";
import { AppLayout, InputField, LinkedAccount, LoadingSpinner } from "../../components";
import { SettingsSection } from "../../components";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";
import { useOAuthFlow } from "../../shared/hooks";
import { userQueries } from "../../state/queries";
import inputFieldClasses from "../../components/input-field/scss/input-field.module.css";
import classes from "./scss/settings.module.css";
import { useFormik } from "formik";
import { updatePasswordSchema } from "../../shared/yup-schemas";
import { Button } from "antd";

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

interface ChangePasswordFormFields {
  currentPassword?: string;
  newPassword: string;
  confirmNewPassword: string;
}

const Settings = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();
  const [isFetchingAccounts, setIsFetchingAccounts] = useState(false);
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const [fetchOAuthAccounts, setFetchOAuthAccounts] = useState(true);
  const [oAuthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([]);
  const { linkProviderAccount } = useOAuthFlow({
    onSuccess: () => {
      showSuccessToast("Account linked successfully");
      setFetchOAuthAccounts(true);
    },
    onError: (error) => {
      showErrorToast(error);
    },
  });

  const unlinkProviderAccount = async (provider: string) => {
    try {
      const response = await requestInstance.post(`/auth/unlink-oauth/${provider}`);
      showSuccessToast(response.data);
      setFetchOAuthAccounts(true);
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  useEffect(() => {
    const fetchAccounts = async () => {
      setIsFetchingAccounts(true);
      try {
        const response = await requestInstance.get("/auth/oauth-providers");
        setOAuthAccounts(response.data);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingAccounts(false);
    };

    if (fetchOAuthAccounts) {
      fetchAccounts();
      setFetchOAuthAccounts(false);
    }
  }, [fetchOAuthAccounts]);

  const changePasswordFormik = useFormik({
    initialValues: (Object.keys(updatePasswordSchema.fields) as (keyof ChangePasswordFormFields)[]).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as ChangePasswordFormFields),
    validationSchema: updatePasswordSchema,
    onSubmit: async (values) => {
      setIsUpdatingPassword(true);
      try {
        const response = await requestInstance.post("/user/change-password", {
          ...values,
        });
        changePasswordFormik.resetForm();
        await refetchUser();
        showSuccessToast(response.data);
      } catch (error: any) {
        showErrorToast(error);
        changePasswordFormik.setFieldValue("newPassword", "");
        changePasswordFormik.setFieldValue("confirmNewPassword", "");
      }
      setIsUpdatingPassword(false);
    },
  });

  return (
    <AppLayout>
      <div className={classes.settings_sections}>
        <SettingsSection title={`${userData?.passwordSet ? "Change" : "Set"} Password`} icon={<Lock />}>
          <form onSubmit={changePasswordFormik.handleSubmit} className={classes.password_form}>
            {userData?.passwordSet && (
              <InputField
                label="Current Password"
                icon={<Lock className={inputFieldClasses.input_icon} />}
                placeholder="Enter your current password"
                name="currentPassword"
                value={changePasswordFormik.values.currentPassword}
                onChange={changePasswordFormik.handleChange}
                onBlur={changePasswordFormik.handleBlur}
                type="password"
                disabled={isUpdatingPassword}
                error={changePasswordFormik.touched.currentPassword && changePasswordFormik.errors.currentPassword ? changePasswordFormik.errors.currentPassword : undefined}
              />
            )}
            <InputField
              label="New Password"
              icon={<Lock className={inputFieldClasses.input_icon} />}
              placeholder="Enter your new password"
              name="newPassword"
              value={changePasswordFormik.values.newPassword}
              onChange={changePasswordFormik.handleChange}
              onBlur={changePasswordFormik.handleBlur}
              type="password"
              disabled={isUpdatingPassword}
              error={changePasswordFormik.touched.newPassword && changePasswordFormik.errors.newPassword ? changePasswordFormik.errors.newPassword : undefined}
            />
            <InputField
              label="Confirm New Password"
              icon={<Lock className={inputFieldClasses.input_icon} />}
              placeholder="Confirm your new password"
              name="confirmNewPassword"
              value={changePasswordFormik.values.confirmNewPassword}
              onChange={changePasswordFormik.handleChange}
              onBlur={changePasswordFormik.handleBlur}
              type="password"
              disabled={isUpdatingPassword}
              error={changePasswordFormik.touched.confirmNewPassword && changePasswordFormik.errors.confirmNewPassword ? changePasswordFormik.errors.confirmNewPassword : undefined}
            />
            <Button
              type="primary"
              htmlType="submit"
              className={classes.password_form_button}
              loading={isUpdatingPassword}
              disabled={!changePasswordFormik.isValid || !changePasswordFormik.dirty || isUpdatingPassword}
            >
              {userData?.passwordSet ? "Update Password" : "Set Password"}
            </Button>
          </form>
        </SettingsSection>
        <SettingsSection title="Linked Accounts" icon={<Link />}>
          {isFetchingAccounts ? (
            <LoadingSpinner />
          ) : (
            oAuthAccounts.map((account, idx) => (
              <LinkedAccount
                key={idx}
                platform={account.provider}
                accountIdentifier={account.email}
                isLinked={account.isLinked}
                onLink={() => linkProviderAccount(account.provider.code)}
                onUnlink={() => unlinkProviderAccount(account.provider.code)}
              />
            ))
          )}
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
