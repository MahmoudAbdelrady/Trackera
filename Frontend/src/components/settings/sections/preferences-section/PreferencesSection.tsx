import { Alert, Button, InputNumber, Select, Spin } from "antd";
import { UserPreference } from "../../../";
import classes from "./scss/preferences-section.module.css";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import { useEffect, useState } from "react";
import type { JiraSite } from "../../../../shared/types";
import { isEqual } from "lodash";
import { userApis } from "../../../../state/api";
import type { TimeZoneOption } from "../../../../shared/types";

// preferences keys
const PreferenceKeys = {
  JIRA_PRIMARY_PROJECT: "jiraPrimaryProject",
  WORKLOGS_MONTHLY_TARGET_HOURS: "worklogsMonthlyTargetHours",
  TIMEZONE: "timeZone",
} as const;

interface PreferencesProps {
  jiraLinked: boolean;
  fetchPreferences: boolean;
  setFetchPreferences: (fetch: boolean) => void;
}

const PreferencesSection = (props: PreferencesProps) => {
  const { jiraLinked, fetchPreferences, setFetchPreferences } = props;

  // user preferences
  const [isLoadingPreferences, setIsLoadingPreferences] = useState<boolean>(false);
  const [initialPreferences, setInitialPreferences] = useState<Record<string, any>>({});
  const [updatedPreferences, setUpdatedPreferences] = useState<Record<string, any>>({});

  // drop-down preferences
  const [isFetchingSites, setIsFetchingSites] = useState<boolean>(false);
  const [jiraSites, setJiraSites] = useState<JiraSite[]>([]);
  const [isFetchingTimeZones, setIsFetchingTimeZones] = useState<boolean>(false);
  const [timeZones, setTimeZones] = useState<TimeZoneOption[]>([]);

  useEffect(() => {
    const fetchUserPreferences = async () => {
      setIsLoadingPreferences(true);
      try {
        const preferences: Record<string, any> = await userApis.getPreferences();

        setInitialPreferences(preferences);
        setUpdatedPreferences(preferences);
        const primarySite = preferences[PreferenceKeys.JIRA_PRIMARY_PROJECT];
        setJiraSites(primarySite ? [primarySite] : []);
        const selectedTimeZone = preferences[PreferenceKeys.TIMEZONE];
        setTimeZones(selectedTimeZone ? [selectedTimeZone] : []);
      } catch (error) {
        showErrorToast(error);
      }
      setIsLoadingPreferences(false);
    };

    if (fetchPreferences) {
      fetchUserPreferences();
      setFetchPreferences(false);
    }
  }, [fetchPreferences, setFetchPreferences]);

  const fetchJiraSites = () =>
    fetchPreferenceOptions<JiraSite>(PreferenceKeys.JIRA_PRIMARY_PROJECT, setIsFetchingSites, setJiraSites);

  const fetchTimeZones = () =>
    fetchPreferenceOptions<TimeZoneOption>(PreferenceKeys.TIMEZONE, setIsFetchingTimeZones, setTimeZones);

  const fetchPreferenceOptions = async <T extends { id: string }>(
    preferenceKey: string,
    setLoading: (loading: boolean) => void,
    setOptions: (options: T[]) => void,
  ) => {
    setLoading(true);
    try {
      const result = await userApis.getPreferenceAllValues(preferenceKey);
      const selectedValue = updatedPreferences[preferenceKey];
      if (selectedValue && !result.find((item: T) => item.id === selectedValue.id)) {
        setOptions([selectedValue, ...result]);
      } else {
        setOptions(result);
      }
    } catch (error: any) {
      showErrorToast(error);
    }
    setLoading(false);
  };

  const handlePreferenceChange = (key: string, value: any) => {
    setUpdatedPreferences((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const transformPreferenceValue = (key: string, value: any): any => {
    if (key === PreferenceKeys.JIRA_PRIMARY_PROJECT) {
      return (value as JiraSite).id;
    }
    if (key === PreferenceKeys.TIMEZONE) {
      return (value as TimeZoneOption).id;
    }
    return value;
  };

  const updateUserPreferences = async () => {
    setIsLoadingPreferences(true);
    try {
      const preferencesToUpdate = Object.fromEntries(
        Object.entries(updatedPreferences)
          .filter(([key, value]) => !isEqual(initialPreferences[key], value))
          .map(([key, value]) => [key, transformPreferenceValue(key, value)]),
      );
      const result = await userApis.updatePreferences(preferencesToUpdate);
      setInitialPreferences(updatedPreferences);
      showSuccessToast(result);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoadingPreferences(false);
  };

  return (
    <div className={classes.preferences_section}>
      {isLoadingPreferences ? (
        <Spin className={classes.loading_spinner} />
      ) : (
        <>
          <UserPreference label="Jira Main Site">
            {jiraLinked ? (
              <Select
                options={jiraSites.map((site) => ({ label: site.name, value: site.id }))}
                value={updatedPreferences[PreferenceKeys.JIRA_PRIMARY_PROJECT]?.id}
                className={classes.preference_select}
                loading={isFetchingSites}
                notFoundContent={isFetchingSites ? <Spin size="small" /> : "No Data"}
                onOpenChange={(open) => {
                  if (open) {
                    fetchJiraSites();
                  }
                }}
                onChange={(value) =>
                  handlePreferenceChange(
                    PreferenceKeys.JIRA_PRIMARY_PROJECT,
                    jiraSites.find((site) => site.id === value),
                  )
                }
              />
            ) : (
              <Alert title="Link your Jira account to enable this option." type="info" showIcon />
            )}
          </UserPreference>
          <UserPreference label="Worklog Monthly Target Hours">
            <InputNumber
              value={updatedPreferences[PreferenceKeys.WORKLOGS_MONTHLY_TARGET_HOURS]}
              onChange={(value) => handlePreferenceChange(PreferenceKeys.WORKLOGS_MONTHLY_TARGET_HOURS, value)}
              className={classes.preference_input}
              min={1}
            />
          </UserPreference>
          <UserPreference label="Time Zone">
            <Select
              options={timeZones.map((tz) => ({ label: tz.label, value: tz.id }))}
              value={updatedPreferences[PreferenceKeys.TIMEZONE]?.id}
              className={`${classes.preference_select} ${classes.timezone}`}
              loading={isFetchingTimeZones}
              notFoundContent={isFetchingTimeZones ? <Spin size="small" /> : "No Data"}
              showSearch={{
                filterOption: (input, option) => (option?.label ?? "").toLowerCase().includes(input.toLowerCase()),
              }}
              placeholder="Select Time Zone"
              onOpenChange={(open) => {
                if (open) {
                  fetchTimeZones();
                }
              }}
              onChange={(value) =>
                handlePreferenceChange(
                  PreferenceKeys.TIMEZONE,
                  timeZones.find((tz) => tz.id === value),
                )
              }
            />
          </UserPreference>
          <Button
            type="primary"
            onClick={updateUserPreferences}
            loading={isLoadingPreferences}
            disabled={isEqual(initialPreferences, updatedPreferences)}
            className={classes.update_button}
          >
            Update Preferences
          </Button>
        </>
      )}
    </div>
  );
};

export default PreferencesSection;
